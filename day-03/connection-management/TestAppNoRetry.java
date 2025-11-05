import java.sql.*;
import java.util.Random;

public class TestAppNoRetry {
    public static void main(String[] args) {
        System.out.println("Starting TestAppNoRetry");
        
        String url = "jdbc:postgresql://node1:26257/bookly?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        String user = "root";
        String password = "pass";
        
        int totalTransactions = 0;
        int successfulTransactions = 0;
        int failedTransactions = 0;
        
        try {
            // Run transactions in a loop
            for (int i = 0; i < 10; i++) {
                totalTransactions++;
                boolean success = executeTransactionWithoutRetry(url, user, password);
                if (success) {
                    successfulTransactions++;
                } else {
                    failedTransactions++;
                }
                Thread.sleep(500); // Pause between transactions
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("WITHOUT RETRY - Total: " + totalTransactions + 
                           ", Successful: " + successfulTransactions + 
                           ", Failed: " + failedTransactions);
    }
    
    private static boolean executeTransactionWithoutRetry(String url, String user, String password) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO book (title, author, price, format, publish_date) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "Test Book Without Retry " + System.currentTimeMillis());
                ps.setString(2, "No Retry Test");
                ps.setFloat(3, 19.99f);
                ps.setString(4, "E-book");
                ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                ps.executeUpdate();
            }
            
            conn.commit();
            System.out.println("Transaction successful");
            return true;
        } catch (SQLException e) {
            System.out.println("SQL Exception: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ")");
            return false;
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            return false;
        }
    }
}
