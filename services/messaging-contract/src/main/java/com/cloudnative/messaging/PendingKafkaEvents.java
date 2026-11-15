package com.cloudnative.messaging;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

/** Durable single-writer queue. Entries are removed only after broker acknowledgement. */
public final class PendingKafkaEvents {
  private final Path directory;

  public PendingKafkaEvents(Path directory) {
    this.directory = directory;
  }

  public void add(String key, String payload) throws IOException {
    Files.createDirectories(directory);
    Path temporary = Files.createTempFile(directory, "pending-", ".tmp");
    try {
      Files.writeString(temporary, key + "\n" + payload, StandardCharsets.UTF_8);
      try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        file.force(true);
      }
      Files.move(temporary, directory.resolve(System.currentTimeMillis() + "-" + UUID.randomUUID() + ".event"),
          StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  public List<Path> pending(int limit) throws IOException {
    Files.createDirectories(directory);
    try (var files = Files.list(directory)) {
      return files.filter(path -> path.getFileName().toString().endsWith(".event"))
          .sorted().limit(limit).toList();
    }
  }

  public Entry read(Path file) throws IOException {
    String text = Files.readString(file, StandardCharsets.UTF_8);
    int separator = text.indexOf('\n');
    if (separator <= 0) throw new IOException("Entrada pendiente inválida");
    return new Entry(text.substring(0, separator), text.substring(separator + 1));
  }

  public void acknowledge(Path file) throws IOException {
    Files.deleteIfExists(file);
  }

  public record Entry(String key, String payload) {}
}
