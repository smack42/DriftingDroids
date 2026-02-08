/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011-2026 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package driftingdroids.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import com.formdev.flatlaf.util.SystemInfo;

/**
 * utility class that attempts to detect whether or not a "dark mode" is active in the operating system.
 * it might work on Windows 10 and 11, MacOS, Linux Gnome and KDE.
 * <p>
 * this code is based on:<br>
 * jSystemThemeDetector https://github.com/Dansoftowner/jSystemThemeDetector <br>
 * Detector https://gist.github.com/HanSolo/7cf10b86efff8ca2845bf5ec2dd0fe1d <br>
 * Detector-Updated https://gist.github.com/IMS212/71f688ba5831bf56e30d0abf0f90e23e <br>
 * FlatLaf https://github.com/JFormDesigner/FlatLaf <br>
 */
public class DarkModeDetector {

    /** the main function of this class.
     * @return true if dark mode is detected in the operating system
     */
    public static boolean isDark() {
        return (isWindows() && isDarkWindows())
                || (isMac() && isDarkMac())
                || (isLinux() && isDarkLinux());
    }



    public static boolean isWindows() {
        return SystemInfo.isWindows;
    }
    public static boolean isDarkWindows() {
        boolean isDark = false;
        final String cmd = "reg query \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme";
        final String regDword = "REG_DWORD";
        try {
            String result = exec(cmd);
            int pos = result.indexOf(regDword);
            if (pos >= 0) {
                // 1 == Light Mode, 0 == Dark Mode
                String temp = result.substring(pos + regDword.length()).trim();
                isDark = ((Integer.parseInt(temp.substring("0x".length()), 16))) == 0;
            }
        } catch (Exception ignored) {
            //ignored
        }
        return isDark;
    }



    public static boolean isMac() {
        return SystemInfo.isMacOS;
    }
    public static boolean isDarkMac() {
        final String cmd = "defaults read -g AppleInterfaceStyle";
        final Pattern darkThemeNamePattern = Pattern.compile(".*dark.*", Pattern.CASE_INSENSITIVE);
        return darkThemeNamePattern.matcher(exec(cmd)).matches();
    }



    public static boolean isLinux() {
        return SystemInfo.isLinux;
    }
    public static boolean isDarkLinux() {
        boolean isDark = false;
        if (SystemInfo.isGNOME) {
            final String[] cmds = new String[]{
                    "gsettings get org.gnome.desktop.interface gtk-theme",
                    "gsettings get org.gnome.desktop.interface color-scheme" };
            final Pattern darkThemeNamePattern = Pattern.compile(".*dark.*", Pattern.CASE_INSENSITIVE);
            for (String cmd : cmds) {
                if (darkThemeNamePattern.matcher(exec(cmd)).matches()) {
                    isDark = true;
                    break;
                }
            }
        } else if (SystemInfo.isKDE) {
            final String cmd = "kreadconfig5 --file kdeglobals --group General --key ColorScheme";
            final Pattern darkThemeNamePattern = Pattern.compile(".*dark.*", Pattern.CASE_INSENSITIVE);
            isDark = darkThemeNamePattern.matcher(exec(cmd)).matches();
        }
        return isDark;
    }



    /** execute a system command and return its output */
    private static String exec(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String actualReadLine;
                while ((actualReadLine = reader.readLine()) != null) {
                    if (stringBuilder.length() != 0) { stringBuilder.append('\n'); }
                    stringBuilder.append(actualReadLine);
                }
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            System.err.println("Exception caught while executing the OS command: \"" + cmd +"\"");
            e.printStackTrace();
            return "";
        }
    }
}
