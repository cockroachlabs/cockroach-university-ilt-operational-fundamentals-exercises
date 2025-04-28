import java.sql.*;
import java.util.Random;

public class TestWithRetry {
    public static void main(String[] args) {
        System.out.println("Starting TestWithRetry");
        
        String initialUrl = "jdbc:postgresql://node1:26257/bookly?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        String fallbackUrl = "jdbc:postgresql://haproxy:26257/bookly?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        String user = "root";
        String password = "pass";
        
        int totalTransactions = 0;
        int successfulTransactions = 0;
        int failedTransactions = 0;
        int retryAttempts = 0;
        
        try {
            // Run transactions in a loop
            for (int i = 0; i < 10; i++) {
                totalTransactions++;
                boolean success = executeTransactionWithRetry(initialUrl, fallbackUrl, user, password);
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
        
        System.out.println("WITH RETRY - Total: " + totalTransactions + 
                           ", Successful: " + successfulTransactions + 
                           ", Failed: " + failedTransactions +
                           ", Retry attempts: " + retryAttempts);
    }
    
    private static boolean executeTransactionWithRetry(String initialUrl, String fallbackUrl, String user, String password) {
        int maxRetries = 5;
        int retries = 0;
        String currentUrl = initialUrl;
        
        while (retries <= maxRetries) {
            try (Connection conn = DriverManager.getConnection(currentUrl, user, password)) {
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO book (title, author, price, format, publish_date) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setString(1, "Test Book With Retry " + System.currentTimeMillis());
                    ps.setString(2, "Retry Test");
                    ps.setFloat(3, 19.99f);
                    ps.setString(4, "E-book");
                    ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                    ps.executeUpdate();
                }
                
                conn.commit();
                System.out.println("Transaction successful using " + currentUrl);
                return true;
            } catch (SQLException e) {
                retries++;
                System.out.println("SQL Exception: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ")");
                
                if (e.getSQLState().equals("40001") || // Retry transient errors
                    e.getSQLState().equals("08006") || // Connection failure
                    e.getSQLState().equals("57P01") || // Server not accepting clients
                    e.getMessage().contains("connection closed") || 
                    e.getMessage().contains("try another node")) {
                    
                    System.out.println("Retrying transaction (attempt " + retries + " of " + maxRetries + ")");
                    
                    // If node1 isn't accepting clients, switch to the load balancer
                    if (currentUrl.contains("node1") && e.getSQLState().equals("57P01")) {
                        System.out.println("Switching from node1 to load balancer");
                        currentUrl = fallbackUrl;
                    }
                    
                    try {
                        Thread.sleep(200 * retries); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.println("Non-retryable error, giving up");
                    return false;
                }
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
                return false;
            }
        }
        
        System.out.println("Exceeded maximum retries");
        return false;
    }
}
