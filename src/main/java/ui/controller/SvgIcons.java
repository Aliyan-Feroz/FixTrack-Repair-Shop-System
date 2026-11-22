package ui.controller;

import javafx.scene.shape.SVGPath;

/**
 * Visual SVG generator helper for UI controllers.
 * Provides standard vector path constants and icon generator methods.
 */
public class SvgIcons {

    public static final String BOLT = "M13 2L3 14h9l-1 8 10-12h-9l1-8z";
    public static final String DASHBOARD = "M3 3h7v7H3zm11 0h7v7h-7zm0 11h7v7h-7zM3 14h7v7H3z";
    public static final String USERS = "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75";
    public static final String JOB_CARDS = "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2 M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z M9 12h6 M9 16h6";
    public static final String INVENTORY = "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z M3.27 6.96L12 12.01l8.73-5.05 M12 22.08V12";
    public static final String TECHNICIANS = "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z";
    public static final String INVOICES = "M2 6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6zm0 4h20 M6 15h4";
    public static final String REPORTS = "M18 20V10 M12 20V4 M6 20v-6";
    public static final String SUN = "M12 1v2 M12 21v2 M4.22 4.22l1.42 1.42 M18.36 18.36l1.42 1.42 M1 12h2 M21 12h2 M4.22 19.78l1.42-1.42 M18.36 5.64l1.42-1.42 M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z";
    public static final String MOON = "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z";
    public static final String LOGOUT = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9";
    public static final String SEARCH = "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";
    public static final String PLUS = "M12 5v14 M5 12h14";
    public static final String REFRESH = "M1 4v6h6 M3.51 15a9 9 0 1 0 2.13-9.36L1 10";
    public static final String CHECK = "M20 6L9 17l-5-5";
    public static final String ALERT = "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z";
    public static final String DOLLAR = "M12 1v22 M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6";
    public static final String CLOCK = "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2";
    public static final String CHEVRON_RIGHT = "M9 18l6-6-6-6";
    public static final String CLOSE = "M18 6L6 18 M6 6l12 12";

    public static SVGPath createIcon(String pathData, String styleClass) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.getStyleClass().add(styleClass);
        return path;
    }

    public static SVGPath icon(String pathData) {
        return createIcon(pathData, "icon-svg");
    }

    public static SVGPath getIcon(String name) {
        SVGPath path = new SVGPath();
        if (name == null) {
            path.setContent(DASHBOARD);
        } else if (name.equalsIgnoreCase("DASHBOARD")) {
            path.setContent(DASHBOARD);
        } else if (name.equalsIgnoreCase("CUSTOMERS") || name.equalsIgnoreCase("USERS")) {
            path.setContent(USERS);
        } else if (name.equalsIgnoreCase("JOB_CARDS") || name.equalsIgnoreCase("JOBCARDS")) {
            path.setContent(JOB_CARDS);
        } else if (name.equalsIgnoreCase("INVENTORY")) {
            path.setContent(INVENTORY);
        } else if (name.equalsIgnoreCase("TECHNICIANS")) {
            path.setContent(TECHNICIANS);
        } else if (name.equalsIgnoreCase("INVOICING") || name.equalsIgnoreCase("INVOICES")) {
            path.setContent(INVOICES);
        } else if (name.equalsIgnoreCase("REPORTS")) {
            path.setContent(REPORTS);
        } else if (name.equalsIgnoreCase("SUN")) {
            path.setContent(SUN);
        } else if (name.equalsIgnoreCase("MOON")) {
            path.setContent(MOON);
        } else if (name.equalsIgnoreCase("LOGOUT")) {
            path.setContent(LOGOUT);
        } else {
            path.setContent("M4 6h16M4 12h16M4 18h16");
        }
        path.getStyleClass().add("icon-svg");
        return path;
    }
}

