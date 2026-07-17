package dev.vepo.issues.ticket.export;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

final class CallerOwnedOutputStream extends FilterOutputStream {

    CallerOwnedOutputStream(OutputStream output) {
        super(output);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        out.write(bytes, offset, length);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
