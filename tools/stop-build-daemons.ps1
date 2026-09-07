# Stops the Gradle and Kotlin build daemons. They hold gigabytes of heap between builds, which
# starved a 13 GB machine that was also running the servers and the client (2026-09-06 hard
# reset). Run after every build; stop-build-daemons.cmd wraps this for cmd users.
$daemons = Get-CimInstance Win32_Process -Filter "name='java.exe'" |
    Where-Object { $_.CommandLine -match 'GradleDaemon|KotlinCompileDaemon|GradleWorkerMain' }
foreach ($d in $daemons) {
  try {
    Stop-Process -Id $d.ProcessId -Force -ErrorAction Stop
    Write-Host ("stopped " + $d.ProcessId)
  } catch {
    Write-Host ("could not stop " + $d.ProcessId + ": " + $_.Exception.Message)
  }
}
if (-not $daemons) { Write-Host "no build daemons running" }
