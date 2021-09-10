package org.liquibase.ext.persistence.utils;

import liquibase.ui.UIService;
import liquibase.util.SystemUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {

    private final UIService ui;
    private final Long duration;
    private final TimeUnit unit;

    public CommandExecutor(UIService ui, Long duration, TimeUnit unit) {
        this.ui = ui;
        this.duration = duration;
        this.unit = unit;
    }

    public CommandExecutor(UIService ui) {
        this.ui = ui;
        this.duration = 10L;
        this.unit = TimeUnit.MINUTES;
    }

    public void exec(List args) throws InterruptedException, IOException {
        this.exec(args, true);
    }

    public void exec(List args, Boolean showOutput) throws InterruptedException, IOException {

        if (showOutput) {
            this.ui.sendMessage("----------------------------------------------------------------------");
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(args);
        Process process = builder.start();
        try {
            if (!SystemUtil.isWindows()) {
                process.waitFor(this.duration, this.unit);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ( (line = reader.readLine()) != null) {
                if (showOutput) {
                    this.ui.sendMessage(line);
                }
            }

            if (process.isAlive()) {
                throw new IOException("Timed out waiting for command: " + args);
            }

            if (process.exitValue() != 0) {
                BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errLine;
                while ( (errLine = errReader.readLine()) != null) {
                    if (showOutput) {
                        this.ui.sendErrorMessage(errLine);
                    }
                }
                throw new IOException("Command failed: " + this.stringify(args) + ": " + "");
            }
            if (SystemUtil.isWindows()) {
                process.waitFor(this.duration, this.unit);
            }
        } finally {
            process.destroy();
            if (showOutput) {
                this.ui.sendMessage(System.getProperty("line.separator"));
            }
        }
    }

    private String stringify(List $this$stringify) {
        String retString = "";
        String item;
        for(Iterator var4 = $this$stringify.iterator(); var4.hasNext(); retString = retString + item + ' ') {
            item = (String)var4.next();
        }
        return retString;
    }
}
