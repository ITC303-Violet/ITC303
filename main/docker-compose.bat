@ECHO OFF
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
REM Used to allow easy use of docker-compose using both
REM docker for windows and docker toolbox

IF NOT DEFINED DOCKER_TOOLBOX_INSTALL_PATH (
	ECHO Docker for windows
	docker-compose.exe %*
) ELSE (
	ECHO Docker toolbox

	REM We have to convert our path to a unix friendly path
	REM so we can cd to our current path
	REM after launching the docker start.sh

	SET "TARGET_DRIVE=%~d0"
	SET "TARGET_PATH=%~p0"
	CALL :tolower TARGET_DRIVE
	SET "FINAL_TARGET_DRIVE=!TARGET_DRIVE:~0,1!"
	SET "UNIX_TARGET_PATH=/!FINAL_TARGET_DRIVE!!TARGET_PATH:\=/!"

	REM Check to find which version of git is installed
	IF EXIST "%PROGRAMFILES%\Git\bin\bash.exe" (
		SET "GITPATH=%PROGRAMFILES%\Git\bin\bash.exe"
	) ELSE (
		IF EXIST "%PROGRAMFILES(X86)%\Git\bin\bash.exe" (
			SET "GITPATH=%PROGRAMFILES(X86)%\Git\bin\bash.exe"
		) ELSE (
			ECHO Failed to locate Git bash.exe
			GOTO :EOF
		)
		
	)

	CD /D "%DOCKER_TOOLBOX_INSTALL_PATH%"
<<<<<<< HEAD
	"%PROGRAMFILES(X86)%\Git\bin\bash.exe" --login -i "%DOCKER_TOOLBOX_INSTALL_PATH%\start.sh" cd "!FINAL_TARGET_PATH!" ^&^& docker-compose.exe %*
=======
	"!GITPATH!" --login -i "%DOCKER_TOOLBOX_INSTALL_PATH%\start.sh" cd "!UNIX_TARGET_PATH!" ^&^& docker-compose.exe %*
>>>>>>> refs/remotes/origin/master
	CD /D "%~dp0"
)

GOTO :EOF
:tolower
FOR %%L IN (a b c d e f g h i j k l m n o p q r s t u v w x y z) DO SET %1=!%1:%%L=%%L!
GOTO :EOF