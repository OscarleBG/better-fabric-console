/*
 * This file is part of Better Fabric Console, licensed under the MIT License.
 *
 * Copyright (c) 2021 Jason Penilla
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.betterfabricconsole;

import java.nio.file.Paths;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import xyz.jpenilla.betterfabricconsole.remap.Remapper;
import xyz.jpenilla.betterfabricconsole.remap.RemappingRewriter;

public final class ConsoleThread extends Thread {
  private static final String TERMINAL_PROMPT = "> ";
  private static final String STOP_COMMAND = "stop";

  private final DedicatedServer server;
  private final LineReader lineReader;

  public ConsoleThread(final @NonNull DedicatedServer server) {
    super("Console thread");
    this.server = server;
    this.lineReader = this.buildLineReader();
  }

  private @NonNull LineReader buildLineReader() {
    return LineReaderBuilder.builder()
      .appName("Fabric Dedicated Server")
      .variable(LineReader.HISTORY_FILE, Paths.get(".console_history"))
      .completer(new MinecraftCommandCompleter(this.server.getCommands().getDispatcher(), this.server.createCommandSourceStack()))
      .highlighter(new MinecraftCommandHighlighter(this.server.getCommands().getDispatcher(), this.server.createCommandSourceStack()))
      .option(LineReader.Option.INSERT_TAB, false)
      .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
      .option(LineReader.Option.COMPLETE_IN_WORD, true)
      .build();
  }

  public void init() {
    final ConsoleAppender consoleAppender = new ConsoleAppender(this.lineReader);
    consoleAppender.start();

    final Logger logger = (Logger) LogManager.getRootLogger();
    final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    final LoggerConfig loggerConfig = loggerContext.getConfiguration().getLoggerConfig(logger.getName());

    final Remapper remapper = BetterFabricConsole.get().remapper();
    if (remapper != null) {
      consoleAppender.installRewriter(new RemappingRewriter(remapper));
    }

    // replace SysOut appender with ConsoleAppender
    loggerConfig.removeAppender("SysOut");
    loggerConfig.addAppender(consoleAppender, loggerConfig.getLevel(), null);
    loggerContext.updateLoggers();
  }

  @Override
  public void run() {
    BetterFabricConsole.LOGGER.info("Done initializing Better Fabric Console console thread.");
    this.acceptInput();
  }

  private boolean isRunning() {
    return !this.server.isStopped() && this.server.isRunning();
  }

  private void acceptInput() {
    while (this.isRunning()) {
      try {
        final String input = this.lineReader.readLine(TERMINAL_PROMPT).trim();
        if (input.isEmpty()) {
          continue;
        }
        this.server.handleConsoleInput(input, this.server.createCommandSourceStack());
        if (input.equals(STOP_COMMAND)) {
          break;
        }
      } catch (final EndOfFileException | UserInterruptException ex) {
        this.server.handleConsoleInput(STOP_COMMAND, this.server.createCommandSourceStack());
        break;
      }
    }
  }
}
