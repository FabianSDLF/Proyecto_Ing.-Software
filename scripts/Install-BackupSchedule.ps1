param([string]$At = '03:00', [string]$TaskName = 'Pitagoras-RespaldoDiario')
$ErrorActionPreference = 'Stop'
$backupScript = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'Backup-Pitagoras.ps1')).Path
$action = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument ('-NoProfile -NonInteractive -WindowStyle Hidden -File "' + $backupScript + '"')
$trigger = New-ScheduledTaskTrigger -Daily -At $At
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew
# Ejecutar este instalador es una acción explícita del operador. No se ejecuta al instalar la aplicación.
Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Description 'Respaldo diario coherente de MySQL y archivos de PITÁGORAS; requiere Docker iniciado.'
