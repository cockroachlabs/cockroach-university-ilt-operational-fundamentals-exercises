import java.sql.*;
import java.util.Random;
import java.time.LocalDate;

public class TestApp {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://haproxy:26257/bookly?sslmode=require";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // Generate random book data
            Random rand = new Random();
            String[] titles = {"The Great Adventure", "Database Fundamentals", "SQL Mastery",
                    "Distributed Systems", "Cloud Computing", "Java Programming"};
            String[] authors = {"J. Smith", "A. Johnson", "M. Davis", "S. Wilson", "K. Martin"};
            String[] formats = {"Hardcover", "Paperback", "E-book", "Audiobook"};

            String title = titles[rand.nextInt(titles.length)];
            String author = authors[rand.nextInt(authors.length)];
            float price = 9.99f + rand.nextFloat() * 40.0f; // Random price between 9.99 and 49.99
            String format = formats[rand.nextInt(formats.length)];
            LocalDate publishDate = LocalDate.now().minusDays(rand.nextInt(1000)); // Random date in past 1000 days

            // Insert data with retry logic
            boolean success = false;
            int retries = 0;
            while (!success && retries < 5) {
                try {
                    conn.setAutoCommit(false);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO book (title, author, price, format, publish_date) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setString(1, title);
                        ps.setString(2, author);
                        ps.setFloat(3, price);
                        ps.setString(4, format);
                        ps.setDate(5, java.sql.Date.valueOf(publishDate));
                        ps.executeUpdate();
                    }
                    conn.commit();
                    success = true;
                } catch (SQLException e) {
                    conn.rollback();
                    if (e.getSQLState().equals("40001")) { // Retry transaction
                        System.out.println("Transaction retry needed");
                        retries++;
                        Thread.sleep(100 * retries); // Backoff
                    } else {
                        throw e;
                    }
                }
            }

            // Query to verify the insert
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT book_id, title FROM book WHERE title = '" + title + "' AND author = '" + author + "' LIMIT 1")) {
                if (rs.next()) {
                    System.out.println("Test complete. Book ID: " + rs.getString("book_id") + ", Title: " + rs.getString("title"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}