package dev.simulated_team.simulated.util;

import org.lwjgl.sdl.SDLScancode;

/** Converts persisted GLFW physical keys to Minecraft's native SDL physical keys. */
public final class KeyboardScanCodes {
    public static final int UNBOUND = SDLScancode.SDL_SCANCODE_UNKNOWN;
    public static int fromLegacy(int key) {
        if (key >= 65 && key <= 90) return SDLScancode.SDL_SCANCODE_A + key - 65;
        if (key >= 49 && key <= 57) return SDLScancode.SDL_SCANCODE_1 + key - 49;
        if (key >= 290 && key <= 301) return SDLScancode.SDL_SCANCODE_F1 + key - 290;
        if (key >= 302 && key <= 313) return SDLScancode.SDL_SCANCODE_F13 + key - 302;
        if (key >= 321 && key <= 329) return SDLScancode.SDL_SCANCODE_KP_1 + key - 321;
        return switch (key) {
            case 32 -> SDLScancode.SDL_SCANCODE_SPACE;
            case 39 -> SDLScancode.SDL_SCANCODE_APOSTROPHE;
            case 44 -> SDLScancode.SDL_SCANCODE_COMMA;
            case 45 -> SDLScancode.SDL_SCANCODE_MINUS;
            case 46 -> SDLScancode.SDL_SCANCODE_PERIOD;
            case 47 -> SDLScancode.SDL_SCANCODE_SLASH;
            case 48 -> SDLScancode.SDL_SCANCODE_0;
            case 59 -> SDLScancode.SDL_SCANCODE_SEMICOLON;
            case 61 -> SDLScancode.SDL_SCANCODE_EQUALS;
            case 91 -> SDLScancode.SDL_SCANCODE_LEFTBRACKET;
            case 92 -> SDLScancode.SDL_SCANCODE_BACKSLASH;
            case 93 -> SDLScancode.SDL_SCANCODE_RIGHTBRACKET;
            case 96 -> SDLScancode.SDL_SCANCODE_GRAVE;
            case 256 -> SDLScancode.SDL_SCANCODE_ESCAPE;
            case 257 -> SDLScancode.SDL_SCANCODE_RETURN;
            case 258 -> SDLScancode.SDL_SCANCODE_TAB;
            case 259 -> SDLScancode.SDL_SCANCODE_BACKSPACE;
            case 260 -> SDLScancode.SDL_SCANCODE_INSERT;
            case 261 -> SDLScancode.SDL_SCANCODE_DELETE;
            case 262 -> SDLScancode.SDL_SCANCODE_RIGHT;
            case 263 -> SDLScancode.SDL_SCANCODE_LEFT;
            case 264 -> SDLScancode.SDL_SCANCODE_DOWN;
            case 265 -> SDLScancode.SDL_SCANCODE_UP;
            case 266 -> SDLScancode.SDL_SCANCODE_PAGEUP;
            case 267 -> SDLScancode.SDL_SCANCODE_PAGEDOWN;
            case 268 -> SDLScancode.SDL_SCANCODE_HOME;
            case 269 -> SDLScancode.SDL_SCANCODE_END;
            case 280 -> SDLScancode.SDL_SCANCODE_CAPSLOCK;
            case 281 -> SDLScancode.SDL_SCANCODE_SCROLLLOCK;
            case 282 -> SDLScancode.SDL_SCANCODE_NUMLOCKCLEAR;
            case 283 -> SDLScancode.SDL_SCANCODE_PRINTSCREEN;
            case 284 -> SDLScancode.SDL_SCANCODE_PAUSE;
            case 320 -> SDLScancode.SDL_SCANCODE_KP_0;
            case 330 -> SDLScancode.SDL_SCANCODE_KP_PERIOD;
            case 331 -> SDLScancode.SDL_SCANCODE_KP_DIVIDE;
            case 332 -> SDLScancode.SDL_SCANCODE_KP_MULTIPLY;
            case 333 -> SDLScancode.SDL_SCANCODE_KP_MINUS;
            case 334 -> SDLScancode.SDL_SCANCODE_KP_PLUS;
            case 335 -> SDLScancode.SDL_SCANCODE_KP_ENTER;
            case 336 -> SDLScancode.SDL_SCANCODE_KP_EQUALS;
            case 340 -> SDLScancode.SDL_SCANCODE_LSHIFT;
            case 341 -> SDLScancode.SDL_SCANCODE_LCTRL;
            case 342 -> SDLScancode.SDL_SCANCODE_LALT;
            case 343 -> SDLScancode.SDL_SCANCODE_LGUI;
            case 344 -> SDLScancode.SDL_SCANCODE_RSHIFT;
            case 345 -> SDLScancode.SDL_SCANCODE_RCTRL;
            case 346 -> SDLScancode.SDL_SCANCODE_RALT;
            case 347 -> SDLScancode.SDL_SCANCODE_RGUI;
            case 348 -> SDLScancode.SDL_SCANCODE_APPLICATION;
            default -> UNBOUND;
        };
    }

    /** Used only by the legacy codec alternative; new saves always write ScanCode. */
    public static int toLegacy(int key) {
        for (int old = 32; old <= 348; old++) if (fromLegacy(old) == key && key != UNBOUND) return old;
        return -1;
    }

    /** Preserve the original physical-key display text while the network/storage key is a native scan code. */
    public static String editLegacyText(String text, int scanCode) {
        if (scanCode == SDLScancode.SDL_SCANCODE_BACKSPACE) return text.isEmpty() ? text : text.substring(0, text.length() - 1);
        int legacy = toLegacy(scanCode);
        return legacy < 0 ? text : text + (char) legacy;
    }

    private KeyboardScanCodes() { }
}
