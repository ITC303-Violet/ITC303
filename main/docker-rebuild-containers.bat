@ECHO OFF
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

IF NOT DEFINED DOCKER_TOOLBOX_INSTALL_PATH (
	ECHO Docker for windows
	docker-compose build
) ELSE (
	ECHO Docker toolbox
	SET "TARGET_DRIVE=%~d0"
	SET "TARGET_PATH=%~p0"
	CALL :tolower TARGET_DRIVE
	SET "FINAL_TARGET_DRIVE=!TARGET_DRIVE:~0,1!"
	SET "FINAL_TARGET_PATH=/!FINAL_TARGET_DRIVE!!TARGET_PATH:\=/!"

	CD /D "%DOCKER_TOOLBOX_INSTALL_PATH%"
	"%PROGRAMFILES%\Git\bin\bash.exe" --login -i "%DOCKER_TOOLBOX_INSTALL_PATH%\start.sh" cd "!FINAL_TARGET_PATH!" ^&^& docker-compose build
	CD /D "%~dp0"
)

GOTO :EOF
:tolower
FOR %%L IN (a b c d e f g h i j k l m n o p q r s t u v w x y z) DO SET %1=!%1:%%L=%%L!
GOTO :EOF