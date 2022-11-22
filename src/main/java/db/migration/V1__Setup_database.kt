package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.util.zip.CRC32

@Suppress("ClassName")
class V1__Setup_database : BaseJavaMigration() {
    private val sql = """
            CREATE TABLE "horse"
            (
                "uid"        text NOT NULL,
                "father_uid" text,
                "mother_uid" text,
                "gender"     text,
                PRIMARY KEY ("uid")
            );
        """.trimIndent()

    override fun migrate(context: Context) {
        context.connection.prepareStatement(sql).use { it.execute() }
    }

    override fun getChecksum(): Int {
        return CRC32().apply {
            update(sql.toByteArray())
        }.value.toInt()
    }
}
