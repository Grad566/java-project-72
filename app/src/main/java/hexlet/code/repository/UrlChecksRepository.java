package hexlet.code.repository;

import hexlet.code.model.UrlCheck;
import lombok.SneakyThrows;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UrlChecksRepository extends BaseRepository {
    @SneakyThrows
    public static List<UrlCheck> getUrlChecksByUrlId(Long id) {
        var result = new ArrayList<UrlCheck>();
        var sql = "SELECT * FROM url_checks WHERE url_id = ?";
        try (var conn = dataSource.getConnection();
                var prepareStmt = conn.prepareStatement(sql)) {
            prepareStmt.setLong(1, id);
            var resultSet = prepareStmt.executeQuery();
            while (resultSet.next()) {
                var urlCheck = UrlCheck.builder()
                                        .id(resultSet.getLong("id"))
                                        .description(resultSet.getString("description"))
                                        .h1(resultSet.getString("h1"))
                                        .title(resultSet.getString("title"))
                                        .createdAt(resultSet.getTimestamp("created_at"))
                                        .urlId(id)
                                        .statusCode(resultSet.getInt("status_code"))
                                        .build();
                result.add(urlCheck);
            }

        }

        return result;
    }

    public static void save(UrlCheck urlCheck) throws SQLException {
        var sql = "INSERT INTO url_checks (status_code, title, h1, description, url_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        var time = new Timestamp(System.currentTimeMillis());
        try (var conn = dataSource.getConnection();
                var prepareStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            prepareStmt.setInt(1, urlCheck.getStatusCode());
            prepareStmt.setString(2, urlCheck.getTitle());
            prepareStmt.setString(3, urlCheck.getH1());
            prepareStmt.setString(4, urlCheck.getDescription());
            prepareStmt.setLong(5, urlCheck.getUrlId());
            prepareStmt.setTimestamp(6, time);
            prepareStmt.executeUpdate();

            var generatedKeys = prepareStmt.getGeneratedKeys();
            if ((generatedKeys.next())) {
                urlCheck.setId(generatedKeys.getLong(1));
                urlCheck.setCreatedAt(time);
            }
        }
    }

    @SneakyThrows
    public static Map<Long, UrlCheck> getLastChecks() {
        var result = new HashMap<Long, UrlCheck>();
        var sql = "SELECT uc.url_id, uc.status_code, uc.created_at "
                + "FROM url_checks uc "
                + "JOIN ( "
                + "    SELECT url_id, MAX(created_at) AS max_created_at"
                + "    FROM url_checks "
                + "    GROUP BY url_id "
                + ") uc_max ON uc.url_id = uc_max.url_id AND uc.created_at = uc_max.max_created_at;";
        try (var conn = dataSource.getConnection();
             var prepareStmt = conn.prepareStatement(sql)) {
            var resultSet = prepareStmt.executeQuery();
            while (resultSet.next()) {
                var urlCheck = UrlCheck.builder()
                        .createdAt(resultSet.getTimestamp("created_at"))
                        .urlId(resultSet.getLong("url_id"))
                        .statusCode(resultSet.getInt("status_code"))
                        .build();
                result.put(urlCheck.getUrlId(), urlCheck);
            }

        }

        return result;
    }
}
