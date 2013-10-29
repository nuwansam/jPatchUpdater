jPatchUpdater
=============
This is a small utility that provides patch creation and updating a folder using a patch. There are 2 modes: create and apply.

for create, pass command line arguments as follows:

java -jar jPatchUpdater.jar create "oldDir" "newDir" "tempFolder" "zipFile"

This command will create a patch file from analyzing the diff between old and new dirs. The tempFolder will be used to temporarily create the diff and it will be deleted after the process. zip file is the file path to the patch zip file required.

for applying a patch created using this, pass command line arguments as follows:

java -jar jPatchUpdater.jar apply "patchFile" "toDir" "tempDir"
