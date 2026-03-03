c$OutputEncoding = [System.Text.Encoding]::UTF8
$out = @()

$out += "=== PORT CHECK ==="
$ports = netstat -ano | Select-String ":808[01]"
if ($ports) { $out += $ports } else { $out += "Neither port 8080 nor 8081 is listening!" }

$out += ""
$out += "=== JAVA PROCESSES ==="
$java = Get-Process java -ErrorAction SilentlyContinue
if ($java) { $java | ForEach-Object { $out += "PID: $($_.Id)  Mem: $([math]::Round($_.WorkingSet/1MB))MB" } }
else { $out += "No Java processes running." }

$out += ""
$out += "=== TEST 1: Side-backend direct call ==="
try {
    $body = @{
        strategy = "GREEDY"
        users = @(@{
            id = 1; firstName = "Alice"; lastName = "Smith"
            email = "alice@test.com"; dailyAvailabilityHours = 8; maxTasks = 5
            roles = @("WORKER"); vacations = @()
        })
        tasks = @(@{
            id = 1; title = "Test Task"; description = "desc"
            durationHours = 4; deadline = "2026-12-31T23:59:59"
            priority = "HIGH"; priorityLevel = 3; status = "PENDING"
            requiredRoles = @("WORKER"); predecessorTaskIds = @(); successorTaskIds = @()
        })
    } | ConvertTo-Json -Depth 5

    $r1 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/algo/schedule" `
        -Method POST -ContentType "application/json" -Body $body -TimeoutSec 10
    $out += "STATUS: 200 OK"
    $out += "Strategy used: $($r1.strategyUsed)"
    $out += "Total tasks: $($r1.totalTasks) | Assigned: $($r1.assignedTasks) | Unassigned: $($r1.unassignedTasks)"
    $r1.assignments | ForEach-Object {
        $out += "  Task[$($_.taskId)] '$($_.taskTitle)' -> $($_.assignedUserFullName ?? 'UNASSIGNED') | $($_.reason)"
    }
} catch {
    $out += "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $out += "Response body: $($reader.ReadToEnd())"
    }
}

$out += ""
$out += "=== TEST 2: Validation error (empty users) ==="
try {
    $body2 = '{"strategy":"GREEDY","users":[],"tasks":[]}'
    $r2 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/algo/schedule" `
        -Method POST -ContentType "application/json" -Body $body2 -TimeoutSec 10
    $out += "ERROR: Expected 400 but got 200"
} catch {
    $out += "Correctly rejected with: $($_.Exception.Message)"
}

$out += ""
$out += "=== TEST 3: Login to main-backend ==="
try {
    $loginBody = '{"nationalId":"admin","password":"admin"}'
    $login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method POST -ContentType "application/json" -Body $loginBody -TimeoutSec 10
    $token = $login.accessToken
    $out += "LOGIN OK — token (first 50): $($token.Substring(0, [Math]::Min(50,$token.Length)))..."

    $out += ""
    $out += "=== TEST 4: Full integration — main-backend -> side-backend ==="
    $headers = @{ Authorization = "Bearer $token" }
    $r4 = Invoke-RestMethod -Uri "http://localhost:8080/api/schedule/run?strategy=GREEDY" `
        -Method POST -Headers $headers -TimeoutSec 15
    $out += "STATUS: 200 OK"
    $out += "Strategy used: $($r4.strategyUsed)"
    $out += "Total: $($r4.totalTasks) | Assigned: $($r4.assignedTasks) | Unassigned: $($r4.unassignedTasks)"
    $r4.assignments | ForEach-Object {
        $out += "  Task[$($_.taskId)] '$($_.taskTitle)' -> $($_.assignedUserFullName ?? 'UNASSIGNED')"
    }
} catch {
    $out += "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $out += "Response body: $($reader.ReadToEnd())"
    }
}

$out | Out-File -FilePath "C:\Users\Osher\Documents\GitHub\Secure-Schedule-Optimization-Sandbox-Project\debug-results.txt" -Encoding utf8
Write-Host "Results written to debug-results.txt"

