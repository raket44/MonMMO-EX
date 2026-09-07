@echo off
rem Stops the Gradle and Kotlin build daemons: they hold gigabytes of heap between builds, which
rem starved a 13 GB machine running the servers and the client (2026-09-06). Run after every build.
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"name=java.exe\" | Where-Object { $_.CommandLine -match GradleDaemon|KotlinCompileDaemon } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Host (stopped  + $_.ProcessId) }"
echo Build daemons stopped.
