# Smoke test of the Expense Tracker API against a running instance.
# Usage: start the app, then run:
#   powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1

$Base = "http://localhost:8080"
$script:Passed = 0
$script:Failed = 0

function Get-AuthHeader($user, $pass) {
    "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${user}:${pass}"))
}

function Invoke-Api($method, $path, $user, $pass, $body) {
    $headers = @{}
    if ($user) { $headers["Authorization"] = Get-AuthHeader $user $pass }
    $params = @{ Method = $method; Uri = ($Base + $path); Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $body) {
        $params["Body"] = [Text.Encoding]::UTF8.GetBytes($body)
        $params["ContentType"] = "application/json"
    }
    try {
        $r = Invoke-WebRequest @params
        $location = $r.Headers["Location"]
        if ($location -is [array]) { $location = $location[0] }
        return @{ Status = [int]$r.StatusCode; Body = [string]$r.Content; Location = $location }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw }
        $content = ""
        if ($_.ErrorDetails) { $content = $_.ErrorDetails.Message }
        return @{ Status = [int]$resp.StatusCode; Body = $content; Location = $null }
    }
}

function Test-Check($name, $condition) {
    if ($condition) {
        Write-Host "[OK]    $name" -ForegroundColor Green
        $script:Passed++
    } else {
        Write-Host "[FALLO] $name" -ForegroundColor Red
        $script:Failed++
    }
}

function Get-List($json) {
    $parsed = ConvertFrom-Json $json
    if ($null -eq $parsed) { return ,@() }
    return ,@($parsed | ForEach-Object { $_ })
}

function PathOf($location) { ([Uri]$location).AbsolutePath }

# --- Is the app running? ---
try { Invoke-Api "GET" "/expenses" $null $null $null | Out-Null }
catch {
    Write-Host "No se puede conectar con $Base. Arranca la aplicacion y vuelve a ejecutar el script." -ForegroundColor Yellow
    exit 1
}

Write-Host "`n== Seguridad ==" -ForegroundColor Cyan
$r = Invoke-Api "GET" "/expenses" $null $null $null
Test-Check "Sin credenciales -> 401 (obtenido $($r.Status))" ($r.Status -eq 401)
$r = Invoke-Api "GET" "/expenses" "lucas" "mala" $null
Test-Check "Contrasena incorrecta -> 401 (obtenido $($r.Status))" ($r.Status -eq 401)
$r = Invoke-Api "GET" "/expenses" "noowner" "qrs456" $null
Test-Check "Usuario sin rol -> 403 (obtenido $($r.Status))" ($r.Status -eq 403)

Write-Host "`n== Crear gastos (POST) ==" -ForegroundColor Cyan
$r = Invoke-Api "GET" "/expenses" "lucas" "abc123" $null
Test-Check "La lista empieza vacia (obtenido $($r.Body))" ($r.Status -eq 200 -and (Get-List $r.Body).Count -eq 0)

$r = Invoke-Api "POST" "/expenses" "lucas" "abc123" '{"description":"Supermercado","amount":45.30,"date":"2026-11-03","category":"Comida"}'
Test-Check "Crear gasto -> 201 con cabecera Location (obtenido $($r.Status))" ($r.Status -eq 201 -and $r.Location)
$supermarketPath = PathOf $r.Location

$r1 = Invoke-Api "POST" "/expenses" "lucas" "abc123" '{"description":"Cine","amount":12.00,"date":"2026-11-01","category":"Ocio"}'
$r2 = Invoke-Api "POST" "/expenses" "lucas" "abc123" '{"description":"Matricula curso","amount":150.00,"date":"2026-10-28","category":"Educacion"}'
$r3 = Invoke-Api "POST" "/expenses" "ana" "xyz789" '{"description":"Gasolina","amount":60.00,"date":"2026-11-02","category":"Transporte"}'
Test-Check "Crear 3 gastos mas (2 de lucas, 1 de ana) -> 201" ($r1.Status -eq 201 -and $r2.Status -eq 201 -and $r3.Status -eq 201)
$anaPath = PathOf $r3.Location

$r = Invoke-Api "POST" "/expenses" "lucas" "abc123" '{"description":"Intento trampa","amount":1.00,"date":"2026-11-04","category":"Ocio","owner":"ana"}'
$trickPath = PathOf $r.Location
$g = Invoke-Api "GET" $trickPath "lucas" "abc123" $null
Test-Check "El dueno lo pone el servidor, no el JSON (owner = lucas)" ($g.Status -eq 200 -and ($g.Body | ConvertFrom-Json).owner -eq "lucas")
Invoke-Api "DELETE" $trickPath "lucas" "abc123" $null | Out-Null

Write-Host "`n== Consultar (GET) ==" -ForegroundColor Cyan
$r = Invoke-Api "GET" $supermarketPath "lucas" "abc123" $null
$e = $r.Body | ConvertFrom-Json
Test-Check "Leer un gasto -> 200 con sus datos" ($r.Status -eq 200 -and $e.description -eq "Supermercado" -and [double]$e.amount -eq 45.3 -and $e.owner -eq "lucas")

$r = Invoke-Api "GET" "/expenses" "lucas" "abc123" $null
$list = (Get-List $r.Body)
Test-Check "Lista de lucas: 3 gastos (obtenido $($list.Count))" ($list.Count -eq 3)
Test-Check "Orden por defecto: el mas reciente primero" ($list[0].description -eq "Supermercado")
Test-Check "La lista no incluye gastos de ana" (-not ($list | Where-Object { $_.owner -ne "lucas" }))

$r = Invoke-Api "GET" "/expenses?sort=amount,desc" "lucas" "abc123" $null
$list = (Get-List $r.Body)
Test-Check "Orden por importe descendente: primero 150" ([double]$list[0].amount -eq 150)

$r = Invoke-Api "GET" "/expenses?page=0&size=2" "lucas" "abc123" $null
Test-Check "Paginacion: size=2 devuelve 2" ((Get-List $r.Body).Count -eq 2)
$r = Invoke-Api "GET" "/expenses?page=1&size=2" "lucas" "abc123" $null
Test-Check "Paginacion: page=1 devuelve el resto (1)" ((Get-List $r.Body).Count -eq 1)

$r = Invoke-Api "GET" "/expenses/99999" "lucas" "abc123" $null
Test-Check "Gasto inexistente -> 404 (obtenido $($r.Status))" ($r.Status -eq 404)
$r = Invoke-Api "GET" $anaPath "lucas" "abc123" $null
Test-Check "Gasto de otro usuario -> 404 (obtenido $($r.Status))" ($r.Status -eq 404)

Write-Host "`n== Resumen ==" -ForegroundColor Cyan
$r = Invoke-Api "GET" "/expenses/summary" "lucas" "abc123" $null
$s = $r.Body | ConvertFrom-Json
Test-Check "Total de lucas = 207.30 (obtenido $($s.total))" ($r.Status -eq 200 -and [double]$s.total -eq 207.3)
Test-Check "3 categorias y sin la de ana" (@($s.byCategory.PSObject.Properties).Count -eq 3 -and -not $s.byCategory.Transporte)

Write-Host "`n== Editar (PUT) ==" -ForegroundColor Cyan
$r = Invoke-Api "PUT" $supermarketPath "lucas" "abc123" '{"description":"Supermercado grande","amount":60.00,"date":"2026-11-03","category":"Comida"}'
Test-Check "Editar gasto propio -> 204 (obtenido $($r.Status))" ($r.Status -eq 204)
$e = (Invoke-Api "GET" $supermarketPath "lucas" "abc123" $null).Body | ConvertFrom-Json
Test-Check "Los cambios se han guardado" ($e.description -eq "Supermercado grande" -and [double]$e.amount -eq 60)
$r = Invoke-Api "PUT" $anaPath "lucas" "abc123" '{"description":"Hackeado","amount":1.00,"date":"2026-11-02","category":"Transporte"}'
Test-Check "Editar gasto de otro -> 404 (obtenido $($r.Status))" ($r.Status -eq 404)
$e = (Invoke-Api "GET" $anaPath "ana" "xyz789" $null).Body | ConvertFrom-Json
Test-Check "El gasto de ana sigue intacto" ($e.description -eq "Gasolina")

Write-Host "`n== Validacion ==" -ForegroundColor Cyan
$r = Invoke-Api "POST" "/expenses" "lucas" "abc123" '{"description":"","amount":-5,"date":"2026-11-04","category":"Comida"}'
Test-Check "Datos invalidos -> 400 (obtenido $($r.Status))" ($r.Status -eq 400)
Write-Host "        Respuesta: $($r.Body)"
$r = Invoke-Api "POST" "/expenses" "lucas" "abc123" '{"description":"Sin fecha","amount":5,"category":"Comida"}'
Test-Check "Falta la fecha -> 400 (obtenido $($r.Status))" ($r.Status -eq 400)
$r = Invoke-Api "POST" "/expenses" "lucas" "abc123" '{ esto no es json'
Test-Check "JSON mal formado -> 400 (obtenido $($r.Status))" ($r.Status -eq 400)

Write-Host "`n== Borrar (DELETE) ==" -ForegroundColor Cyan
$r = Invoke-Api "DELETE" $anaPath "lucas" "abc123" $null
Test-Check "Borrar gasto de otro -> 404 (obtenido $($r.Status))" ($r.Status -eq 404)
$r = Invoke-Api "DELETE" $supermarketPath "lucas" "abc123" $null
Test-Check "Borrar gasto propio -> 204 (obtenido $($r.Status))" ($r.Status -eq 204)
$r = Invoke-Api "GET" $supermarketPath "lucas" "abc123" $null
Test-Check "El gasto borrado ya no existe -> 404" ($r.Status -eq 404)
$r = Invoke-Api "DELETE" $supermarketPath "lucas" "abc123" $null
Test-Check "Borrarlo otra vez -> 404" ($r.Status -eq 404)

Write-Host "`n========================================"
$color = if ($script:Failed -eq 0) { "Green" } else { "Red" }
Write-Host "Resultado: $script:Passed correctas, $script:Failed fallidas" -ForegroundColor $color
Write-Host "Reinicia la app antes de volver a ejecutar el script (la base de datos debe empezar vacia)."
