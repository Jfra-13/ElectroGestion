@echo off
echo ========================================
echo Ejecutando Pruebas del Backend...
echo ========================================
call .\mvnw.cmd test
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] TODAS LAS PRUEBAS PASARON CORRECTAMENTE.
) else (
    echo.
    echo [ERROR] HUBO ERRORES EN LAS PRUEBAS.
)
pause
