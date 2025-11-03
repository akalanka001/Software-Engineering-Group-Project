package Admin;

import javax.swing.JOptionPane;

public final class Alerts {
    private Alerts() {}
    public static void success(String msg){ JOptionPane.showMessageDialog(null,msg,"Success",JOptionPane.INFORMATION_MESSAGE); }
    public static void fail(String msg){ JOptionPane.showMessageDialog(null,msg,"Error",JOptionPane.ERROR_MESSAGE); }
    public static void info(String msg){ JOptionPane.showMessageDialog(null,msg); }
}