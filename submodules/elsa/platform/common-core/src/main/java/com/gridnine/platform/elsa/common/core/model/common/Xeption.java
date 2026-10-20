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

package com.gridnine.platform.elsa.common.core.model.common;

public class Xeption extends Error {
    private XeptionType type;
    private String developerMessage;
    private L10nMessage adminMessage;
    private L10nMessage endUserMessage;

    public Xeption(String message, Throwable cause, XeptionType type, String developerMessage, L10nMessage adminMessage, L10nMessage endUserMessage) {
        super(message, cause);
        this.type = type;
        this.developerMessage = developerMessage;
        this.adminMessage = adminMessage;
        this.endUserMessage = endUserMessage;
    }

    public static Xeption forDeveloper(String message) {
        return forDeveloper(message, null);
    }

    public static Xeption forAdmin(L10nMessage adminMessage, Throwable e) {
        return new Xeption(adminMessage.toString(), e, XeptionType.FOR_ADMIN, null, adminMessage, null);
    }

    public static Xeption forAdmin(L10nMessage adminMessage) {
        return forAdmin(adminMessage, null);
    }

    public static Xeption forDeveloper(String message, Exception cause) {
        return new Xeption(message, cause, XeptionType.FOR_DEVELOPER, message, null, null);
    }

    public static Xeption forEndUser(L10nMessage endUserMessage) {
        return new Xeption(endUserMessage.toString(), null, XeptionType.FOR_END_USER, null, null, endUserMessage);
    }

    public XeptionType getType() {
        return type;
    }

    public void setType(XeptionType type) {
        this.type = type;
    }

    public String getDeveloperMessage() {
        return developerMessage;
    }

    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    public L10nMessage getAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(L10nMessage adminMessage) {
        this.adminMessage = adminMessage;
    }

    public L10nMessage getEndUserMessage() {
        return endUserMessage;
    }

    public void setEndUserMessage(L10nMessage endUserMessage) {
        this.endUserMessage = endUserMessage;
    }
}
