package com.xs0.gqlktx.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * A helper ID thing for use with Relay-compliant schemas. Contains a type part,
 * and an array of id parts, which can be/are type-specific.
 * <p>
 * The string encoding is produced by converting the whole thing into a binary encoding
 * (see {@link PackedIdListWriter)), then base64url-encoding that.
 * <p>
 * For example, a simple item id might be something like <code>[ "item", "SKU0001" ]</code>,
 * while a row in a link table between three other tables might be identified with
 * something like <code>[ "abc_link", 515, 914781, 2545 ]</code>.
 */
public final class NodeId {
    private final String typeId;
    private final String encoded;
    private final Object[] parts;

    private NodeId(String typeId, Object[] parts, String encoded) {
        this.typeId = typeId;
        this.parts = parts;
        this.encoded = encoded;
    }

    public String getTypeId() {
        return typeId;
    }

    public boolean matches(String typeId, Class<?>... partTypes) {
        if (!typeId.equals(this.typeId))
            return false;

        if (partTypes.length != parts.length)
            return false;

        for (int i = 0; i < parts.length; i++) {
            if (!partTypes[i].isAssignableFrom(parts[i].getClass()))
                return false;
        }

        return true;
    }

    public <T> T getPart(int i, Class<T> type) {
        return type.cast(parts[i]);
    }

    @Override
    public String toString() {
        return typeId + Arrays.asList(parts).toString();
    }

    public String toPublicId() {
        return encoded;
    }

    public static Builder create(String typeId) {
        return new Builder(typeId);
    }

    public static NodeId fromPublicID(String encodedId) {
        PackedIdListReader reader = new PackedIdListReader(Base64.getDecoder().wrap(new StringInputStream(encodedId)));

        try {
            Object typeIdObj = reader.readNext();
            if (typeIdObj instanceof String) {
                String typeId = (String) typeIdObj;
                Object[] parts = reader.readRest();
                if (parts.length < 1)
                    throw new IllegalArgumentException("Missing id parts");

                return new NodeId(typeId, parts, encodedId);
            } else {
                throw new IllegalArgumentException("NodeId didn't start with typeId (a string)");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read encodedId", e);
        }
    }

    public int numParts() {
        return parts.length;
    }

    public static class Builder {
        private final String typeId;
        private final StringOutputStream encoded;
        private final OutputStream base64;
        private final PackedIdListWriter partWriter;
        private final ArrayList<Object> parts;

        Builder(String typeId) {
            this.typeId = typeId;
            this.encoded = new StringOutputStream();
            this.base64 = Base64.getEncoder().withoutPadding().wrap(encoded);
            this.partWriter = new PackedIdListWriter(base64);
            this.parts = new ArrayList<>();

            try {
                this.partWriter.writeString(typeId);
            } catch (IOException e) {
                throw new Error(e); // the underlying output stream (this.encoded) does not throw IOException..
            }
        }

        public NodeId build() {
            if (parts.isEmpty())
                throw new IllegalStateException("No identifiers");

            try {
                base64.close();
            } catch (IOException e) {
                throw new Error(e);
            }

            return new NodeId(typeId, parts.toArray(), encoded.toString());
        }

        public Builder add(boolean value) {
            parts.add(value);
            try {
                partWriter.writeBoolean(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(byte value) {
            parts.add(value);
            try {
                partWriter.writeByte(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(short value) {
            parts.add(value);
            try {
                partWriter.writeShort(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(char value) {
            parts.add(value);
            try {
                partWriter.writeChar(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(int value) {
            parts.add(value);
            try {
                partWriter.writeInt(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(long value) {
            parts.add(value);
            try {
                partWriter.writeLong(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(float value) {
            parts.add(value);
            try {
                partWriter.writeFloat(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(double value) {
            parts.add(value);
            try {
                partWriter.writeDouble(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(UUID value) {
            parts.add(value);
            try {
                partWriter.writeUUID(value);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }

        public Builder add(String s) {
            parts.add(s);
            try {
                partWriter.writeString(s);
            } catch (IOException e) {
                throw new Error(e);
            }
            return this;
        }
    }
}
