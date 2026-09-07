from pathlib import Path

root = Path('modern')

pom = root / 'pom.xml'
text = pom.read_text(encoding='utf-8')
text = text.replace('<version>1.0.14</version>', '<version>1.0.15</version>', 1)
pom.write_text(text, encoding='utf-8')

redis = root / 'src/main/java/com/revature/project0/security/RedisRateLimitStore.java'
text = redis.read_text(encoding='utf-8')
old = 'return new RateLimitDecision(count <= limit, Math.max(0, limit - count), Math.max(1, ttl));'
new = 'int remaining = (int) Math.max(0L, (long) limit - count);\n        return new RateLimitDecision(count <= limit, remaining, Math.max(1L, ttl));'
if old not in text:
    raise SystemExit('RedisRateLimitStore expected v1.0.14 line not found')
redis.write_text(text.replace(old, new, 1), encoding='utf-8')

offer = root / 'src/main/java/com/revature/project0/springjdbc/SpringJdbcOfferRepository.java'
offer.write_text('''package com.revature.project0.springjdbc;\n\nimport com.revature.project0.domain.Offer;\nimport com.revature.project0.domain.OfferStatus;\nimport com.revature.project0.repository.OfferRepository;\nimport java.sql.ResultSet;\nimport java.sql.SQLException;\nimport java.util.List;\nimport java.util.Optional;\nimport java.util.UUID;\nimport org.springframework.dao.OptimisticLockingFailureException;\nimport org.springframework.jdbc.core.JdbcTemplate;\nimport org.springframework.stereotype.Repository;\n\n@Repository\npublic final class SpringJdbcOfferRepository implements OfferRepository {\n    private final JdbcTemplate jdbc;\n\n    public SpringJdbcOfferRepository(JdbcTemplate jdbc) {\n        this.jdbc = jdbc;\n    }\n\n    @Override\n    public Offer save(Offer offer) {\n        int updated = jdbc.update(\n                "INSERT INTO offer(id,customer_id,vehicle_id,amount,status,decided_by_employee_id,row_version) " +\n                "VALUES (?,?,?,?,?,?,?) " +\n                "ON CONFLICT(id) DO UPDATE SET amount=EXCLUDED.amount,status=EXCLUDED.status," +\n                "decided_by_employee_id=EXCLUDED.decided_by_employee_id,row_version=EXCLUDED.row_version " +\n                "WHERE offer.row_version=EXCLUDED.row_version-1",\n                offer.getId(), offer.getCustomerId(), offer.getVehicleId(), offer.getAmount(),\n                offer.getStatus().name(), offer.getDecidedByEmployeeId(), offer.getVersion());\n        if (updated == 0) {\n            throw new OptimisticLockingFailureException("offer changed concurrently");\n        }\n        return offer;\n    }\n\n    @Override\n    public Optional<Offer> findById(UUID id) {\n        return jdbc.query("SELECT * FROM offer WHERE id=?", this::map, id).stream().findFirst();\n    }\n\n    @Override\n    public List<Offer> findByVehicleIdAndStatus(UUID vehicleId, OfferStatus status) {\n        return jdbc.query(\n                "SELECT * FROM offer WHERE vehicle_id=? AND status=? ORDER BY amount DESC",\n                this::map, vehicleId, status.name());\n    }\n\n    @Override\n    public List<Offer> findByCustomerId(UUID customerId) {\n        return jdbc.query(\n                "SELECT * FROM offer WHERE customer_id=? ORDER BY id",\n                this::map, customerId);\n    }\n\n    @Override\n    public List<Offer> findByStatus(OfferStatus status) {\n        return jdbc.query(\n                "SELECT * FROM offer WHERE status=? ORDER BY vehicle_id,amount DESC",\n                this::map, status.name());\n    }\n\n    @Override\n    public List<Offer> findAll() {\n        return jdbc.query("SELECT * FROM offer ORDER BY id", this::map);\n    }\n\n    private Offer map(ResultSet rs, int row) throws SQLException {\n        return new Offer(\n                rs.getObject("id", UUID.class),\n                rs.getObject("customer_id", UUID.class),\n                rs.getObject("vehicle_id", UUID.class),\n                rs.getBigDecimal("amount"),\n                OfferStatus.valueOf(rs.getString("status")),\n                rs.getObject("decided_by_employee_id", UUID.class),\n                rs.getLong("row_version"));\n    }\n}\n''', encoding='utf-8')

print('V1_0_15_PATCH_APPLIED')
