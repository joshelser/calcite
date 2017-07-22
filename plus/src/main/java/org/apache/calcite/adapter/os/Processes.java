/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.adapter.os;

import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Utilities regarding operating system processes.
 *
 * <p>WARNING: Spawning processes is not secure.
 * Use this class caution.
 * This class is in the "plus" module because "plus" is not used by default.
 * Do not move this class to the "core" module.
 */
public class Processes {
  private Processes() {}

  /** Executes a command and returns its result as an enumerable of lines. */
  static Enumerable<String> processLines(String... args) {
    return new ProcessLinesEnumerator(args);
  }

  /** Enumerator that executes a process and returns each line as an element. */
  private static class ProcessLinesEnumerator
      extends AbstractEnumerable<String> {
    private final String[] args;

    ProcessLinesEnumerator(String... args) {
      this.args = args;
    }

    public Enumerator<String> enumerator() {
      final String command = args[0];
      try {
        final Process process = new ProcessBuilder().command(args).start();
        final InputStream is = process.getInputStream();
        final InputStreamReader isr =
            new InputStreamReader(is, StandardCharsets.UTF_8);
        final BufferedReader br = new BufferedReader(isr);
        return new Enumerator<String>() {
          private String line;

          public String current() {
            return line;
          }

          public boolean moveNext() {
            try {
              line = br.readLine();
              return line != null;
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }

          public void reset() {
            throw new UnsupportedOperationException();
          }

          public void close() {
            try {
              br.close();
            } catch (IOException e) {
              throw new RuntimeException("while running " + command, e);
            }
            process.destroy();
          }
        };
      } catch (IOException e) {
        throw new RuntimeException("while running " + command, e);
      }
    }
  }
}

// End Processes.java
