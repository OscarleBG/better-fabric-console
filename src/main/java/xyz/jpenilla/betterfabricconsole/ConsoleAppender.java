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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jline.reader.LineReader;

final class ConsoleAppender extends AbstractAppender {
  private final LineReader lineReader;
  private RewritePolicy rewriter = null;

  ConsoleAppender(final @NonNull LineReader lineReader) {
    super(
      "Console",
      null,
      PatternLayout.newBuilder().withPattern(BetterFabricConsole.get().config().logPattern()).build(),
      false,
      new Property[0]
    );
    this.lineReader = lineReader;
  }

  public void installRewriter(final @Nullable RewritePolicy rewriter) {
    this.rewriter = rewriter;
  }

  private @NonNull LogEvent rewrite(final @NonNull LogEvent event) {
    if (this.rewriter == null) {
      return event;
    }
    return this.rewriter.rewrite(event);
  }

  @Override
  public void append(final @NonNull LogEvent event) {
    if (this.lineReader.isReading()) {
      this.lineReader.callWidget(LineReader.CLEAR);
    }

    this.lineReader.getTerminal().writer().print(this.getLayout().toSerializable(this.rewrite(event)).toString());

    if (this.lineReader.isReading()) {
      this.lineReader.callWidget(LineReader.REDRAW_LINE);
      this.lineReader.callWidget(LineReader.REDISPLAY);
    }
    this.lineReader.getTerminal().writer().flush();
  }
}
