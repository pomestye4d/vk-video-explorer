/*
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gridnine.platform.elsa.common.core.serialization;

public class SerializationParameters {

    private boolean prettyPrint = true;

    private EntityReferenceCaptionSerializationStrategy entityReferenceCaptionSerializationStrategy = EntityReferenceCaptionSerializationStrategy.ALL;

    private EntityReferenceTypeSerializationStrategy entityReferenceTypeSerializationStrategy = EntityReferenceTypeSerializationStrategy.ALL_CLASS_NAME;

    private EnumSerializationStrategy enumSerializationStrategy = EnumSerializationStrategy.NAME;

    private ClassSerializationStrategy classSerializationStrategy = ClassSerializationStrategy.NAME;

    public ClassSerializationStrategy getClassSerializationStrategy() {
        return classSerializationStrategy;
    }

    public EmptyListSerializationStrategy emptyListSerializationStrategy;

    public EmptyListSerializationStrategy getEmptyListSerializationStrategy() {
        return emptyListSerializationStrategy;
    }

    public SerializationParameters setEmptyListSerializationStrategy(EmptyListSerializationStrategy emptyListSerializationStrategy) {
        this.emptyListSerializationStrategy = emptyListSerializationStrategy;
        return this;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public SerializationParameters setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        return this;
    }

    public EntityReferenceCaptionSerializationStrategy getEntityReferenceCaptionSerializationStrategy() {
        return entityReferenceCaptionSerializationStrategy;
    }

    public SerializationParameters setClassSerializationStrategy(ClassSerializationStrategy classSerializationStrategy) {
        this.classSerializationStrategy = classSerializationStrategy;
        return this;
    }

    public SerializationParameters setEntityReferenceCaptionSerializationStrategy(EntityReferenceCaptionSerializationStrategy entityReferenceCaptionSerializationStrategy) {
        this.entityReferenceCaptionSerializationStrategy = entityReferenceCaptionSerializationStrategy;
        return this;
    }

    public EntityReferenceTypeSerializationStrategy getEntityReferenceTypeSerializationStrategy() {
        return entityReferenceTypeSerializationStrategy;
    }

    public SerializationParameters setEntityReferenceTypeSerializationStrategy(EntityReferenceTypeSerializationStrategy entityReferenceTypeSerializationStrategy) {
        this.entityReferenceTypeSerializationStrategy = entityReferenceTypeSerializationStrategy;
        return this;
    }

    public SerializationParameters setEnumSerializationStrategy(EnumSerializationStrategy enumSerializationStrategy) {
        this.enumSerializationStrategy = enumSerializationStrategy;
        return this;
    }

    public EnumSerializationStrategy getEnumSerializationStrategy() {
        return enumSerializationStrategy;
    }

    public enum EntityReferenceCaptionSerializationStrategy {
        ALL,
        ONLY_NOT_CACHED
    }

    public enum EntityReferenceTypeSerializationStrategy {
        ALL_CLASS_NAME,
        ABSTRACT_CLASS_ID
    }

    public enum EnumSerializationStrategy {
        ID,
        NAME
    }

    public enum ClassSerializationStrategy {
        ID,
        NAME
    }

    public enum EmptyListSerializationStrategy {
        SKIP,
        INCLUDE
    }
}
