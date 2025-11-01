package Lecturer;

import javax.swing.JTable;

/**
 * A simple CRUD operation interface using
 * standard Swing JTable instead of RSTableMetro.
 */
public interface CRUD_Operation_Interface {

    /**
     * Loads and populates table data from a database or other source.
     *
     * @param table any JTable to fill with rows and columns
     * @param searchValue optional search string (may be empty)
     * @param lecId lecturer ID or other identifier
     */
    void getDetails(JTable table, String searchValue, String lecId);
}