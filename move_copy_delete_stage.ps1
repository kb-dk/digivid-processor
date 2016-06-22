# Et powershellscript til flytning, kopiering og sletning af filer fra videodigitalisering
# Bruger et modul, der hedder "PSCX 3.2.0 Production" til Get-Hash. https://pscx.codeplex.com/

Write-Output "Starter"

[string]$windir1="C:\digitized_recordings\"
[string]$windir2="C:\preprocessed_digitized_recordings\"
[string]$SAMBAshare="\\halley\vhsfile-stage\"
[string]$commentsFileExt=".comments"
[string]$doneFileExt=".done"
[string]$persistentTempExt=".tmp"
[int]$retainDays=14

#Funktioner
function CopyCompareWren {
	param([string]$Source, [string]$Destination, [string]$Filename, [string]$LogFile, [switch]$Rename, [switch]$Move)
	
	#Tjek om der skal renames
	if ($Rename) {
		[string]$tempExt=$persistentTempExt
	} else {
		[string]$tempExt=$null
	}
	
	# Kopierer filen fra source til destination
	Write-Output "Kopierer $Source$Filename til $Destination$Filename$tempExt"
	Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] Copying $Source$Filename to $Destination$Filename$tempExt" 
	Copy-Item $Source$Filename $Destination$Filename$tempExt -ErrorAction SilentlyContinue –errorvariable copyerror
	if ($copyerror -ne $null) {
		Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [ERROR] $copyerror"
		Write-Output "Der opstod en kopieringsfejl."
		exit 112
	}
	Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] Done"
	
	#Sammenligner indholdet sourcefilen og destinationsfilen
	Write-Output "Sammenligner indholdet af $Source$Filename og $Destination$Filename$tempExt"
	Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] Comparing content of $Source$Filename and $Destination$Filename$tempExt" 
	if ((Get-Hash $Source$Filename -Algorithm MD5 | select -ExpandProperty "HashString") -ne (Get-Hash $Destination$Filename$tempExt -Algorithm MD5 | select -ExpandProperty "HashString")) {
		Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [ERROR] MD5 hash of $Source$Filename does not equal $Destination$Filename$tempExt"
		Write-Output "Der opstod en MD5-fejl."
		exit 112
	}
	Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] MD5 hash match"
	
	#Rename fra tempExt til oprindeligt navn
	if ($Rename) {
		Write-Output "Rename $Destination$Filename$tempExt til $Destination$Filename"
		Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] Renaming $Destination$Filename$tempExt to $Destination$Filename" 
		Rename-Item $Destination$Filename$tempExt $Destination$Filename -ErrorAction SilentlyContinue –errorvariable renameerror
		if ($renameerror -ne $null) {
			Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [ERROR] $renameerror"
			Write-Output "Der opstod en renamefejl."
			exit 112
		}
		Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] Done"
	}
	
	#Hvis filen skal flyttes, så skal source slettes
	if ($Move) {
		#DeleteFile $Source $Filename $LogFile
        Remove-Item $Source$Filename -ErrorAction Stop -ErrorVariable $deleteError
		if ($deleteError -ne $null) {
			Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [ERROR] $deleteError"
			Write-Output "Der opstod en sletnings fejl."
			exit 112
		}
	}
}

function DeleteFile {
	param([string]$Source, [string]$Filename, [string]$LogFile)

	#Sletter den ønskede fil
		Write-Output "Sletter $Source$Filename"
		Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] Deleting $Source$Filename" 
		Remove-Item $Source$Filename -ErrorAction SilentlyContinue –errorvariable removeerror
		if ($removeerror -ne $null) {
			Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [ERROR] $removeerror"
			Write-Output "Der opstod en slettefejl."
			exit 112
		}
		Add-Content $LogFile "$(Get-Date -format HH:mm:ss.fff) [info] Done"
}
# Stopper Digivid processor, hvis det kører
#Stop-Process -Name java -Force 

# Flytter alle comments-filer med tilhørende filer til windir2
foreach ($commentsFile in get-childitem -path $windir1 -filter *$commentsFileExt) {
	$transportstreamFile=(Get-ChildItem $windir1$commentsFile).BaseName
	$LogFile="move$(Get-Date -Format yyyyMMdd).log"
	
	CopyCompareWren $windir1 $windir2 $transportstreamFile $windir1$LogFile -move
	CopyCompareWren $windir1 $windir2 $commentsFile $windir1$LogFile -move
}

#Kopierer alle comments-filer med tilhørende transportstream-filer til SAMBAshare
foreach ($commentsFile in get-childitem -path $windir2 -filter *$commentsFileExt) {
	$transportstreamFile=(Get-ChildItem $windir2$commentsFile).BaseName
	$LogFile="copy$(Get-Date -Format yyyyMMdd).log"
	
	# Hvis der ikke er en tilsvarende fil med doneFileExt, så skal filerne kopieres
	If (Test-Path $windir2$transportstreamFile$doneFileExt){
		Add-Content $windir2$LogFile "$(Get-Date -format HH:mm:ss.fff) [info] $windir2$transportstreamFile$doneFileExt exists. Next, please!"
	} else {
		CopyCompareWren $windir2 $SAMBAshare $transportstreamFile $windir2$LogFile
		CopyCompareWren $windir2 $SAMBAshare $commentsFile $windir2$LogFile -rename
		
		# Lav en fil som markør for veludført kopiering af både comments-filen og transportstram-filen med extention som defineret i doneFileExt
		Add-Content $windir2$transportstreamFile$doneFileExt "$(Get-Date -format HH:mm:ss.fff) [info] $windir2$transportstreamFile and $windir2$commentsFile Done"
	}
}

#Sletter alle comments-filer med tilhørende transportstream-filer og donefilext ældre end retainDays dage fra windir2
#dato for sidste ændring i donefile
foreach ($doneFile in (get-childitem -path $windir2 -filter *$doneFileExt | where-object {$_.LastWriteTime -lt (get-date).AddDays(-$retainDays)})) {
	$transportstreamFile=(Get-ChildItem $windir2$doneFile).BaseName
	$LogFile="cleanup$(Get-Date -Format yyyyMMdd).log"

	DeleteFile $windir2 $transportstreamFile$commentsFileExt $windir2$LogFile
	DeleteFile $windir2 $transportstreamFile $windir2$LogFile
	DeleteFile $windir2 $doneFile $windir2$LogFile
	}

# Start Digivid processor
cd C:\digivid-processor
#java --% -Ddigivid.config=config/digivid-processor.properties -jar digivid-processor-1.1-jfx.jar

Write-Output "Slutter"