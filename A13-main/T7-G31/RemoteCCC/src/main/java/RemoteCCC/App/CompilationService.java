/*
 *   Copyright (c) 2024 Stefano Marano https://github.com/StefanoMarano80017
 *   All rights reserved.

 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package RemoteCCC.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompilationService {
    /*
     * Config gestisce i path delle directory e fornisce un id univoco 
     * serve solo per non tenere tutta la logica qua
     */
    private final Config config;
    private final String testingClassName;
    private final String testingClassCode;
    private final String underTestClassName;
    private final String underTestClassCode;
    /*
     *  Qui metto tutte le variabili che ottengo come output
     *  Attenzione outputMaven viene settato da compileExecuteCoverageWithMaven()
     *  mentre errors e Coverage in base al suo valore vengono settati da compileAndTest
     */
    public String outputMaven;
    public Boolean Errors;
    public String Coverage;
    //logger
    private static final Logger logger = LoggerFactory.getLogger(CompilationService.class);

    public CompilationService(String testingClassName, String testingClassCode,
            String underTestClassName, String underTestClassCode) {
        this.config = new Config();
        this.testingClassName = testingClassName;
        this.testingClassCode = testingClassCode;
        this.underTestClassName = underTestClassName;
        this.underTestClassCode = underTestClassCode;
        this.outputMaven = null;
        logger.info("[CompilationService] Servizi creato con successo");
    }

    public void compileAndTest() throws IOException, InterruptedException {
        try {
            createDirectoriesAndCopyPom();
            saveCodeToFile(this.testingClassName, this.testingClassCode, config.getTestingClassPath());
            saveCodeToFile(this.underTestClassName, this.underTestClassCode, config.getUnderTestClassPath());
            logger.info("[CompilationService] Avvio Maven");
            if (compileExecuteCoverageWithMaven()) {
                this.Coverage = readFileToString(config.getCoverageFolderPath());
                this.Errors = false;
                logger.info("[Compilation Service] Compilazione Terminata senza errori.");
            } else {
                this.Coverage = null;
                this.Errors = true;
                logger.info("[Compilation Service] Compilazione Terminata con errori");
            }
            deleteFile(config.getUnderTestClassPath() + underTestClassName);
            deleteFile(config.getTestingClassPath() + testingClassName);
            deleteTemporaryDirectories(config.getPathCompiler());
            logger.info("[CompilationService] File termporanei eliminati");
        } catch (FileConcurrencyException e) {
            logger.error("[Compilation Service] [LOCK ERROR] ", e);
        } catch (IOException e) {
            logger.error("[Compilation Service] [I/O ERROR] ", e);
        } catch (IllegalArgumentException e) {
            logger.error("[Compilation Service] [ARGS ERROR] ", e);
        } catch (RuntimeException e) {
            logger.error("[Compilation Service] [RUNTIME ERROR] ", e);
        } catch (Exception e) {
            logger.error("[Compilation Service] [GENERIC ERROR] ", e);
        }
    }

    private void createDirectoriesAndCopyPom() throws IOException {
        /*
             *   Creo la cartella usrPath/timestamp/
             *   poi creo  usrPath/timestamp/src/main/java/
             *   e usrPath/timestamp/test/main/java/
             *   infine per il report della cov userPath/timestamp/target/site/
         */
        createDirectoryIfNotExists(config.getPathCompiler());
        logger.info("[Compilation Service] directory creata con successo: {}", config.getPathCompiler());

        copyPomFile();
        logger.info("[Compilation Service] pom file creato con successo {}", config.getPathCompiler() + "pom.xml");

        createDirectoryIfNotExists(config.getTestingClassPath());
        logger.info("[Compilation Service] directory creata con successo: {}", config.getTestingClassPath());

        createDirectoryIfNotExists(config.getUnderTestClassPath());
        logger.info("[Compilation Service] directory creata con successo: {}", config.getUnderTestClassPath());

        createDirectoryIfNotExists(config.getCoverageFolderPath());
        logger.info("[Compilation Service] directory creata con successo: {}", config.getCoverageFolderPath());
    }

    private void createDirectoryIfNotExists(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("[createDirectoryIfNotExists] Impossibile creare la directory: " + path);
            }
        } else {
            throw new IOException("[createDirectoryIfNotExists] Errore esiste già la directory: " + path);
        }
    }

    private void copyPomFile() throws IOException {
        File pomFile = new File(config.getUsrPath() + config.getsep() + "ClientProject" + config.getsep() + "pom.xml");
        File destPomFile = new File(config.getPathCompiler() + "pom.xml");
        File lockFile = new File(config.getPathCompiler() + "pom.lock"); // File di lock

        // Controlla se il file pom.xml esiste prima di tentare di copiarlo
        if (!pomFile.exists()) {
            throw new IOException("[Compilation Service] Il file pom.xml non esiste: " + pomFile.getAbsolutePath());
        } else {
            //Col filechannel rende l'operazione atomica
            try (
                 FileChannel lockChannel   = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE); 
                 FileChannel sourceChannel = FileChannel.open(pomFile.toPath(), StandardOpenOption.READ); 
                 FileChannel destChannel   = FileChannel.open(destPomFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                ) {
                // Acquisisci un lock sul file di lock
                try (FileLock lock = lockChannel.lock()) {
                    // Acquisisci un lock sul file sorgente per evitare accessi concorrenti
                    try (FileLock sourceLock = sourceChannel.lock(0, Long.MAX_VALUE, true)) {
                        // Copia il contenuto
                        long size = sourceChannel.size();
                        long position = 0;

                        while (position < size) {
                            position += sourceChannel.transferTo(position, size - position, destChannel);
                        }
                    } catch (OverlappingFileLockException e) {
                        throw new FileConcurrencyException("[copyPomFile] FileLock concorrente sul file sorgente: " + e.getMessage(), e);
                    } catch (FileLockInterruptionException e) {
                        throw new FileConcurrencyException("[copyPomFile] L'acquisizione del lock è stata interrotta: " + e.getMessage(), e);
                    } catch (NonWritableChannelException e) {
                        throw new FileConcurrencyException("[copyPomFile] Canale di scrittura non valido per acquisire il lock: " + e.getMessage(), e);
                    } catch (ClosedChannelException e) {
                        throw new FileConcurrencyException("[copyPomFile] Il canale è stato chiuso prima di acquisire il lock: " + e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                throw new IOException("[copyPomFile] Errore copia pom.xml: " + e.getMessage(), e);
            }
        }
    }

    private void saveCodeToFile(String nameclass, String code, String path) throws IOException {
        // Controlla che il nome della classe e il percorso siano validi
        if (nameclass == null || nameclass.isEmpty()) {
            throw new IllegalArgumentException("[saveCodeToFile] Il nome della classe non può essere nullo o vuoto.");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("[saveCodeToFile] Il percorso non può essere nullo o vuoto.");
        }
        // Aggiungi la dichiarazione del pacchetto al codice
        String packageDeclaration = config.getPackageDeclaration();
        code = packageDeclaration + code;
        //creo il file di destinazione 
        File tempFile = new File(path + nameclass);
        // Imposta il file per la cancellazione all'uscita
        tempFile.deleteOnExit();
        // Utilizza FileChannel per scrivere in modo atomico
        try (FileChannel channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            // Acquisisci un lock sul file per evitare accessi concorrenti
            try (FileLock lock = channel.lock()) {
                // Scrivi il codice nel file
                ByteBuffer buffer = StandardCharsets.UTF_8.encode(code);
                channel.write(buffer);
            } catch (OverlappingFileLockException e) {
                throw new FileConcurrencyException("[saveCodeToFile] FileLock concorrente sul file durante la scrittura: " + e.getMessage(), e);
            } catch (FileLockInterruptionException e) {
                throw new FileConcurrencyException("[saveCodeToFile] L'acquisizione del lock è stata interrotta: " + e.getMessage(), e);
            } catch (NonWritableChannelException e) {
                throw new FileConcurrencyException("[saveCodeToFile] Canale di scrittura non valido per acquisire il lock: " + e.getMessage(), e);
            } catch (ClosedChannelException e) {
                throw new FileConcurrencyException("[saveCodeToFile] Il canale è stato chiuso prima di acquisire il lock: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new IOException("[saveCodeToFile] Errore durante la scrittura nel file: " + tempFile.getAbsolutePath(), e);
        }
    }

    private boolean compileExecuteCoverageWithMaven() throws RuntimeException{
        ProcessBuilder processBuilder = new ProcessBuilder("mvn", "clean", "compile", "test");
        processBuilder.directory(new File(config.getPathCompiler()));
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        Process process = null;
        try {
            // Avvia il processo
            process = processBuilder.start();
            // Leggi l'output standard e l'errore in thread separati per evitare deadlock
            readProcessOutput(process, output, errorOutput);
            // Attendi la fine del processo con un timeout (es. 15 minuti)
            boolean finished = process.waitFor(15, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly(); // Uccidi il processo se supera il timeout
                throw new RuntimeException("[compileExecuteCoverageWithMaven] Timeout superato. Il processo Maven è stato forzatamente interrotto.");
            }
            // Verifica se il processo è terminato con successo
            if ((process.exitValue()) == 0){
                // Aggiungi l'output Maven alla variabile membro
                this.outputMaven += output.toString();
                return true;
            } else {
                this.outputMaven += errorOutput.toString();
                return false;
            }
        } catch (IOException e) {
            logger.error("[Compilation Service] [MAVEN] {}", errorOutput);
            throw new RuntimeException("[compileExecuteCoverageWithMaven] Errore di I/O durante l'esecuzione del processo Maven: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("[Compilation Service] [MAVEN] {}", errorOutput);
            Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
            throw new RuntimeException("[compileExecuteCoverageWithMaven] Processo Maven interrotto: " + e.getMessage(), e);
        } finally {
            // Assicurati che il processo sia terminato correttamente
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void readProcessOutput(Process process, StringBuilder output, StringBuilder errorOutput) throws IOException, InterruptedException {
        try (
             BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream())); 
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
            ) {
            // Leggi l'output standard
            String line;
            while ((line = outputReader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            // Leggi l'output di errore
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append(System.lineSeparator());
            }
        }
    }

    private void deleteFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("[deleteFile] Impossibile eliminare il file: " + file.getAbsolutePath());
            }
        } else {
            throw new IllegalArgumentException("[deleteFile] Il file non esiste: " + file.getAbsolutePath());
        }
    }

    private void deleteTemporaryDirectories(String path) throws IOException {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                throw new IOException("[deleteTemporaryDirectories] Errore durante l'eliminazione della directory temporanea: " + dir.getAbsolutePath(), e);
            }
        } else {
            throw new IOException("[deleteTemporaryDirectories] La directory non esiste o non è valida: " + dir.getAbsolutePath());
        }
    }

    private String readFileToString(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes);
    }

    //mi serve per distingure le eccezioni sulla concorrenza 
    public class FileConcurrencyException extends IOException {

        public FileConcurrencyException(String message) {
            super(message);
        }

        public FileConcurrencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
