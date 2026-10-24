
package com.gymmanagement.gymmanagement

import android.R
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val DB_NAME = "gym_management.db"
        private const val DB_VERSION = 5
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE gym_settings (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                prefix TEXT NOT NULL DEFAULT 'AC'
            )
        """)

        db.execSQL("""
            CREATE TABLE members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_code TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                email TEXT DEFAULT '',
                gender TEXT DEFAULT '',
                dob TEXT DEFAULT '',
                address TEXT DEFAULT '',
                emergency_contact TEXT DEFAULT '',
                emergency_phone TEXT DEFAULT '',
                joining_date TEXT DEFAULT '',
                notes TEXT DEFAULT '',
                photo_path TEXT DEFAULT '',
                height_cm REAL,
                weight_kg REAL,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL
            )   
        """)

        db.execSQL("""
            CREATE TABLE membership_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                months INTEGER NOT NULL DEFAULT 1,
                price REAL NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                is_trial INTEGER NOT NULL DEFAULT 0,
                plan_type TEXT NOT NULL DEFAULT 'INDIVIDUAL',
                member_count INTEGER NOT NULL DEFAULT 1
            )
        """)

        db.execSQL("""
            CREATE TABLE memberships (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id INTEGER NOT NULL,
                plan_id INTEGER NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT NOT NULL,
                plan_amount REAL NOT NULL DEFAULT 0,
                discount_type TEXT NOT NULL DEFAULT 'NONE',
                discount_value REAL NOT NULL DEFAULT 0,
                discount_amount REAL NOT NULL DEFAULT 0,
                total_amount REAL NOT NULL DEFAULT 0,
                paid_amount REAL NOT NULL DEFAULT 0,
                balance_amount REAL NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'Active',
                frozen INTEGER NOT NULL DEFAULT 0,
                is_trial INTEGER NOT NULL DEFAULT 0,
                group_id INTEGER,
                created_at TEXT NOT NULL,
                FOREIGN KEY(member_id) REFERENCES members(id),
                FOREIGN KEY(plan_id) REFERENCES membership_plans(id)
            )
        """)

        db.execSQL("""
            CREATE TABLE payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id INTEGER NOT NULL,
                membership_id INTEGER,
                amount REAL NOT NULL,
                method TEXT NOT NULL DEFAULT 'Cash',
                reference TEXT DEFAULT '',
                notes TEXT DEFAULT '',
                paid_at TEXT NOT NULL,
                FOREIGN KEY(member_id) REFERENCES members(id)
            )
        """)

        db.execSQL("""
            CREATE TABLE attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id INTEGER NOT NULL,
                check_in TEXT NOT NULL,
                check_out TEXT,
                workout_hours REAL NOT NULL DEFAULT 0,
                source TEXT NOT NULL DEFAULT 'Manual',
                FOREIGN KEY(member_id) REFERENCES members(id)
            )
        """)

        db.execSQL("""
            CREATE TABLE trainers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT DEFAULT '',
                email TEXT DEFAULT '',
                specialization TEXT DEFAULT '',
                active INTEGER NOT NULL DEFAULT 1
            )
        """)

        db.execSQL("""
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                description TEXT DEFAULT '',
                amount REAL NOT NULL,
                expense_date TEXT NOT NULL,
                notes TEXT DEFAULT ''
            )
        """)

        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'Receptionist',
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE member_documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id INTEGER NOT NULL,
                document_name TEXT NOT NULL,
                document_path TEXT NOT NULL,
                uploaded_at TEXT NOT NULL,
                FOREIGN KEY(member_id) REFERENCES members(id)
            )
        """)

        db.execSQL("CREATE INDEX idx_members_code ON members(member_code)")
        db.execSQL("CREATE INDEX idx_members_phone ON members(phone)")
        db.execSQL("CREATE INDEX idx_memberships_member ON memberships(member_id)")
        db.execSQL("CREATE INDEX idx_payments_member ON payments(member_id)")
        db.execSQL("CREATE INDEX idx_attendance_member ON attendance(member_id)")

        db.execSQL("""
            CREATE TABLE membership_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                group_code TEXT UNIQUE NOT NULL,
                plan_id INTEGER NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT NOT NULL,
                plan_amount REAL NOT NULL DEFAULT 0,
                discount_type TEXT NOT NULL DEFAULT 'NONE',
                discount_value REAL NOT NULL DEFAULT 0,
                discount_amount REAL NOT NULL DEFAULT 0,
                total_amount REAL NOT NULL DEFAULT 0,
                paid_amount REAL NOT NULL DEFAULT 0,
                balance_amount REAL NOT NULL DEFAULT 0,
                payment_method TEXT NOT NULL DEFAULT 'Cash',
                created_at TEXT NOT NULL,
                FOREIGN KEY(plan_id) REFERENCES membership_plans(id)
            )
        """)

        db.execSQL("""
            INSERT INTO membership_plans(name, months, price, active, is_trial)
            VALUES('Trial', 0, 0, 1, 1)
        """)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS membership_groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_code TEXT UNIQUE NOT NULL,
                    plan_id INTEGER NOT NULL,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    plan_amount REAL NOT NULL DEFAULT 0,
                    discount_type TEXT NOT NULL DEFAULT 'NONE',
                    discount_value REAL NOT NULL DEFAULT 0,
                    discount_amount REAL NOT NULL DEFAULT 0,
                    total_amount REAL NOT NULL DEFAULT 0,
                    paid_amount REAL NOT NULL DEFAULT 0,
                    balance_amount REAL NOT NULL DEFAULT 0,
                    payment_method TEXT NOT NULL DEFAULT 'Cash',
                    created_at TEXT NOT NULL
                )
            """)

            try {
                db.execSQL(
                    "ALTER TABLE memberships ADD COLUMN group_id INTEGER"
                )
            } catch (_: Exception) {
            }

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_memberships_group ON memberships(group_id)"
            )
        }

        if (oldVersion < 3) {

            try {
                db.execSQL(
                    "ALTER TABLE membership_plans ADD COLUMN plan_type TEXT NOT NULL DEFAULT 'INDIVIDUAL'"
                )
            } catch (_: Exception) {
            }

            try {
                db.execSQL(
                    "ALTER TABLE membership_plans ADD COLUMN member_count INTEGER NOT NULL DEFAULT 1"
                )
            } catch (_: Exception) {
            }
        }

        // Version 4:
        // Add workout duration to existing attendance records.
        if (oldVersion < 4) {

            try {
                db.execSQL(
                    "ALTER TABLE attendance ADD COLUMN workout_hours REAL NOT NULL DEFAULT 0"
                )
            } catch (_: Exception) {
            }
        }

        // Version 5:
        // Add height and weight to existing member records.
        if (oldVersion < 5) {

            try {
                db.execSQL(
                    "ALTER TABLE members ADD COLUMN height_cm REAL"
                )
            } catch (_: Exception) {
            }

            try {
                db.execSQL(
                    "ALTER TABLE members ADD COLUMN weight_kg REAL"
                )
            } catch (_: Exception) {
            }
        }
    }

    fun createDefaultAdmin() {

        val db = writableDatabase

        val c = db.rawQuery(
            "SELECT id FROM users WHERE username='admin' LIMIT 1",
            null
        )

        c.use {

            if (!it.moveToFirst()) {

                val values = ContentValues().apply {
                    put("username", "admin")
                    put("password", "admin123")
                    put("role", "Admin")
                    put("active", 1)
                    put("created_at", now())
                }

                db.insert("users", null, values)
            }
        }
    }

    fun saveGymSettings(
        gymName: String,
        gymPhone: String,
        gymAddress: String,
        memberPrefix: String
    ) {

        val values = ContentValues().apply {
            put("id", 1)
            put("name", gymName.trim())
            put("phone", gymPhone.trim())
            put("address", gymAddress.trim())
            put("prefix", memberPrefix.trim().uppercase(Locale.getDefault()))
        }

        writableDatabase.insertWithOnConflict(
            "gym_settings",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getGymSettings(): Map<String, String> {

        val result = mutableMapOf<String, String>()

        val c = readableDatabase.rawQuery(
            "SELECT name, phone, address, prefix FROM gym_settings WHERE id=1 LIMIT 1",
            null
        )

        c.use {

            if (it.moveToFirst()) {

                result["name"] = it.getString(0) ?: ""
                result["phone"] = it.getString(1) ?: ""
                result["address"] = it.getString(2) ?: ""
                result["prefix"] = it.getString(3) ?: "AC"
            }
        }

        return result
    }

    fun count(table: String): Int {

        val allowed = setOf(
            "members",
            "membership_plans",
            "memberships",
            "payments",
            "attendance",
            "trainers",
            "expenses",
            "users",
            "member_documents"
        )

        if (table !in allowed) return 0

        return scalarInt("SELECT COUNT(*) FROM $table")
    }

    fun scalarInt(sql: String): Int {

        val c = readableDatabase.rawQuery(sql, null)

        c.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun scalarDouble(sql: String): Double {

        val c = readableDatabase.rawQuery(sql, null)

        c.use {
            return if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    fun addMember(
        name: String,
        phone: String,
        email: String,
        gender: String,
        dob: String,
        address: String,
        emergency: String,
        emergencyPhone: String,
        joiningDate: String,
        notes: String,
        photoPath: String,
        prefix: String
    ): String {

        val db = writableDatabase
        val code = generateNextMemberCode(prefix)

        val values = ContentValues().apply {

            put("member_code", code)
            put("name", name.trim())
            put("phone", phone.trim())
            put("email", email.trim())
            put("gender", gender.trim())
            put("dob", dob.trim())
            put("address", address.trim())
            put("emergency_contact", emergency.trim())
            put("emergency_phone", emergencyPhone.trim())
            put("joining_date", joiningDate)
            put("notes", notes.trim())
            put("photo_path", photoPath)
            put("active", 1)
            put("created_at", now())
        }

        val id = db.insertOrThrow(
            "members",
            null,
            values
        )

        // Automatically assign Trial plan.
        val trial = db.rawQuery(
            "SELECT id FROM membership_plans WHERE is_trial=1 LIMIT 1",
            null
        )

        trial.use {

            if (it.moveToFirst()) {

                val planId = it.getLong(0)

                val membership = ContentValues().apply {

                    put("member_id", id)
                    put("plan_id", planId)
                    put("start_date", joiningDate)
                    put("end_date", "9999-12-31")
                    put("plan_amount", 0.0)
                    put("discount_type", "NONE")
                    put("discount_value", 0.0)
                    put("discount_amount", 0.0)
                    put("total_amount", 0.0)
                    put("paid_amount", 0.0)
                    put("balance_amount", 0.0)
                    put("status", "Trial")
                    put("frozen", 0)
                    put("is_trial", 1)
                    put("created_at", now())
                }

                db.insert(
                    "memberships",
                    null,
                    membership
                )
            }
        }

        return code
    }

    fun generateNextMemberCode(prefix: String): String {

        val cleanPrefix =
            prefix.trim()
                .uppercase(Locale.getDefault())
                .ifBlank { "AC" }

        val c = readableDatabase.rawQuery(
            """
            SELECT member_code
            FROM members
            WHERE member_code LIKE ?
            ORDER BY id DESC
            LIMIT 1
            """,
            arrayOf("$cleanPrefix%")
        )

        c.use {

            if (it.moveToFirst()) {

                val old = it.getString(0)

                val number =
                    old.removePrefix(cleanPrefix)
                        .toIntOrNull() ?: 0

                return cleanPrefix +
                        String.format(
                            "%03d",
                            number + 1
                        )
            }
        }

        return cleanPrefix + "001"
    }

    fun getMembers(
        search: String = ""
    ): List<Array<String>> {

        val rows = mutableListOf<Array<String>>()

        val sql = """
            SELECT
                m.member_code,
                m.name,
                m.phone,
                COALESCE(
                    (
                        SELECT p.name
                        FROM membership_plans p
                        JOIN memberships ms
                            ON ms.plan_id=p.id
                        WHERE ms.member_id=m.id
                        ORDER BY ms.id DESC
                        LIMIT 1
                    ),
                    'No Plan'
                ) AS plan_name,
                COALESCE(
                    (
                        SELECT ms.end_date
                        FROM memberships ms
                        WHERE ms.member_id=m.id
                        ORDER BY ms.id DESC
                        LIMIT 1
                    ),
                    ''
                ) AS expiry,
                m.active,
                COALESCE(
                    (
                        SELECT ms.status
                        FROM memberships ms
                        WHERE ms.member_id=m.id
                        ORDER BY ms.id DESC
                        LIMIT 1
                    ),
                    'Inactive'
                ) AS membership_status,
                COALESCE(
                    (
                        SELECT ms.balance_amount
                        FROM memberships ms
                        WHERE ms.member_id=m.id
                        ORDER BY ms.id DESC
                        LIMIT 1
                    ),
                    0
                ) AS balance
            FROM members m
            WHERE (
                ? = ''
                OR m.member_code LIKE ?
                OR m.name LIKE ?
                OR m.phone LIKE ?
            )
            ORDER BY m.id DESC
        """

        val q = "%${search.trim()}%"

        val c = readableDatabase.rawQuery(
            sql,
            arrayOf(
                search.trim(),
                q,
                q,
                q
            )
        )

        c.use {

            while (it.moveToNext()) {

                rows.add(
                    arrayOf(
                        it.getString(0) ?: "",
                        it.getString(1) ?: "",
                        it.getString(2) ?: "",
                        it.getString(3) ?: "",
                        it.getString(4) ?: "",
                        it.getString(5) ?: "1",
                        it.getString(6) ?: "",
                        it.getString(7) ?: "0"
                    )
                )
            }
        }

        return rows
    }

    fun getPlans(): List<Array<String>> {

        val rows = mutableListOf<Array<String>>()

        val c = readableDatabase.rawQuery(
            """
            SELECT
                id,
                name,
                months,
                price,
                active,
                is_trial,
                plan_type,
                member_count
            FROM membership_plans
            ORDER BY is_trial DESC, id DESC
            """,
            null
        )

        c.use {

            while (it.moveToNext()) {

                rows.add(
                    arrayOf(
                        it.getString(0),
                        it.getString(1),
                        it.getString(2),
                        it.getString(3),
                        it.getString(4),
                        it.getString(5),
                        it.getString(6),
                        it.getString(7)
                    )
                )
            }
        }

        return rows
    }

    fun addPlan(
        name: String,
        months: Int,
        price: Double,
        planType: String = "INDIVIDUAL",
        memberCount: Int = 1
    ) {

        if (name.trim().isBlank()) {
            throw IllegalArgumentException("Plan name is required.")
        }

        if (months < 0) {
            throw IllegalArgumentException("Months cannot be negative.")
        }

        if (price < 0) {
            throw IllegalArgumentException("Price cannot be negative.")
        }

        val type = planType.uppercase(Locale.getDefault())

        if (
            type !in setOf(
                "INDIVIDUAL",
                "COUPLE",
                "GROUP"
            )
        ) {
            throw IllegalArgumentException("Invalid plan type.")
        }

        if (
            type == "INDIVIDUAL" &&
            memberCount != 1
        ) {
            throw IllegalArgumentException(
                "Individual plan member count must be 1."
            )
        }

        if (
            type != "INDIVIDUAL" &&
            memberCount < 2
        ) {
            throw IllegalArgumentException(
                "Couple/Group plan must have at least 2 members."
            )
        }

        val values = ContentValues().apply {

            put("name", name.trim())
            put("months", months)
            put("price", price)
            put("active", 1)
            put("is_trial", 0)
            put("plan_type", type)
            put("member_count", memberCount)
        }

        writableDatabase.insertOrThrow(
            "membership_plans",
            null,
            values
        )
    }

    fun togglePlan(id: Long) {

        val db = writableDatabase

        val c = db.rawQuery(
            "SELECT active,is_trial FROM membership_plans WHERE id=?",
            arrayOf(id.toString())
        )

        c.use {

            if (
                it.moveToFirst() &&
                it.getInt(1) == 0
            ) {

                val active =
                    if (it.getInt(0) == 1) 0 else 1

                val values = ContentValues().apply {
                    put("active", active)
                }

                db.update(
                    "membership_plans",
                    values,
                    "id=?",
                    arrayOf(id.toString())
                )
            }
        }
    }

    fun assignMembership(
        memberCode: String,
        planId: Long,
        discountType: String,
        discountValue: Double,
        initialPayment: Double,
        paymentMethod: String
    ): Double {

        val db = writableDatabase

        val memberId =
            findMemberId(memberCode)
                ?: throw IllegalArgumentException(
                    "Member not found."
                )

        val plan = db.rawQuery(
            """
            SELECT name,months,price,active,is_trial
            FROM membership_plans
            WHERE id=?
            """,
            arrayOf(planId.toString())
        )

        var months = 0
        var price = 0.0
        var isTrial = false

        plan.use {

            if (!it.moveToFirst()) {
                throw IllegalArgumentException(
                    "Plan not found."
                )
            }

            if (it.getInt(3) == 0) {
                throw IllegalArgumentException(
                    "This plan is inactive."
                )
            }

            months = it.getInt(1)
            price = it.getDouble(2)
            isTrial = it.getInt(4) == 1
        }

        if (isTrial) {
            months = 0
            price = 0.0
        }

        val type =
            discountType.uppercase(Locale.getDefault())

        val discount = when (type) {

            "%" ->
                price *
                        (
                                discountValue
                                    .coerceIn(0.0, 100.0)
                                        / 100.0
                                )

            "FIXED" ->
                discountValue.coerceIn(
                    0.0,
                    price
                )

            else -> 0.0
        }

        val total =
            (price - discount)
                .coerceAtLeast(0.0)

        val paid =
            initialPayment
                .coerceAtLeast(0.0)
                .coerceAtMost(total)

        val balance =
            (total - paid)
                .coerceAtLeast(0.0)

        val start = today()

        val end =
            if (isTrial) {
                "9999-12-31"
            } else {
                addMonths(start, months)
            }

        db.execSQL(
            """
            UPDATE memberships
            SET status='Inactive'
            WHERE member_id=?
              AND status IN ('Active','Trial')
            """,
            arrayOf(memberId)
        )

        val membership = ContentValues().apply {

            put("member_id", memberId)
            put("plan_id", planId)
            put("start_date", start)
            put("end_date", end)
            put("plan_amount", price)
            put("discount_type", type)
            put("discount_value", discountValue)
            put("discount_amount", discount)
            put("total_amount", total)
            put("paid_amount", paid)
            put("balance_amount", balance)
            put(
                "status",
                if (isTrial)
                    "Trial"
                else if (balance <= 0.005)
                    "Active"
                else
                    "Pending"
            )
            put("frozen", 0)
            put(
                "is_trial",
                if (isTrial) 1 else 0
            )
            put("created_at", now())
        }

        val membershipId =
            db.insertOrThrow(
                "memberships",
                null,
                membership
            )

        if (paid > 0.0) {

            val payment = ContentValues().apply {

                put("member_id", memberId)
                put("membership_id", membershipId)
                put("amount", paid)
                put("method", paymentMethod)
                put("reference", "")
                put(
                    "notes",
                    "Initial membership payment"
                )
                put("paid_at", now())
            }

            db.insert(
                "payments",
                null,
                payment
            )
        }

        return balance
    }

    fun getMembersForGroup(
        search: String = ""
    ): List<Array<String>> {

        val rows = mutableListOf<Array<String>>()

        val q = "%${search.trim()}%"

        val c = readableDatabase.rawQuery(
            """
            SELECT member_code,name,phone
            FROM members
            WHERE active=1
              AND (
                  member_code LIKE ?
                  OR name LIKE ?
                  OR phone LIKE ?
              )
            ORDER BY name COLLATE NOCASE
            """,
            arrayOf(q, q, q)
        )

        c.use {

            while (it.moveToNext()) {

                rows.add(
                    arrayOf(
                        it.getString(0),
                        it.getString(1),
                        it.getString(2)
                    )
                )
            }
        }

        return rows
    }

    fun assignGroupMembership(
        memberCodes: List<String>,
        planId: Long,
        discountType: String,
        discountValue: Double,
        initialPayment: Double,
        paymentMethod: String
    ): Double {

        val codes =
            memberCodes
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        if (codes.size < 2) {
            throw IllegalArgumentException(
                "Select at least 2 members for a Couple / Group plan."
            )
        }

        val db = writableDatabase

        var months = 0
        var price = 0.0
        var isTrial = false
        var planType = "GROUP"
        var configuredMemberCount = 2

        val plan = db.rawQuery(
            """
            SELECT months,price,active,is_trial,plan_type,member_count
            FROM membership_plans
            WHERE id=?
            """,
            arrayOf(planId.toString())
        )

        plan.use {

            if (!it.moveToFirst()) {
                throw IllegalArgumentException(
                    "Plan not found."
                )
            }

            if (it.getInt(2) == 0) {
                throw IllegalArgumentException(
                    "This plan is inactive."
                )
            }

            months = it.getInt(0)
            price = it.getDouble(1)
            isTrial = it.getInt(3) == 1
            planType = it.getString(4) ?: "GROUP"
            configuredMemberCount =
                it.getInt(5).coerceAtLeast(2)
        }

        if (
            planType !in listOf(
                "COUPLE",
                "GROUP"
            )
        ) {
            throw IllegalArgumentException(
                "This is not a Couple / Group plan."
            )
        }

        if (
            codes.size > configuredMemberCount
        ) {
            throw IllegalArgumentException(
                "This plan allows up to $configuredMemberCount members."
            )
        }

        if (
            planType == "COUPLE" &&
            codes.size != 2
        ) {
            throw IllegalArgumentException(
                "Couple plan requires exactly 2 members."
            )
        }

        if (isTrial) {
            throw IllegalArgumentException(
                "Trial plan cannot be assigned as a group plan."
            )
        }

        val type =
            discountType.uppercase(Locale.getDefault())

        val discount = when (type) {

            "%" ->
                price *
                        (
                                discountValue
                                    .coerceIn(0.0, 100.0)
                                        / 100.0
                                )

            "FIXED" ->
                discountValue.coerceIn(
                    0.0,
                    price
                )

            else -> 0.0
        }

        val total =
            (price - discount)
                .coerceAtLeast(0.0)

        val paid =
            initialPayment
                .coerceAtLeast(0.0)
                .coerceAtMost(total)

        val balance =
            (total - paid)
                .coerceAtLeast(0.0)

        val start = today()
        val end = addMonths(start, months)

        val memberIds =
            codes.map {
                findMemberId(it)
                    ?: throw IllegalArgumentException(
                        "Member not found: $it"
                    )
            }

        db.beginTransaction()

        try {

            memberIds.forEach { id ->

                db.execSQL(
                    """
                    UPDATE memberships
                    SET status='Inactive'
                    WHERE member_id=?
                      AND status IN ('Active','Trial','Pending')
                    """,
                    arrayOf(id)
                )
            }

            val groupCode =
                "GRP-" +
                        SimpleDateFormat(
                            "yyyyMMddHHmmssSSS",
                            Locale.US
                        ).format(Date())

            val groupValues =
                ContentValues().apply {

                    put("group_code", groupCode)
                    put("plan_id", planId)
                    put("start_date", start)
                    put("end_date", end)
                    put("plan_amount", price)
                    put("discount_type", type)
                    put("discount_value", discountValue)
                    put("discount_amount", discount)
                    put("total_amount", total)
                    put("paid_amount", paid)
                    put("balance_amount", balance)
                    put(
                        "payment_method",
                        paymentMethod
                    )
                    put("created_at", now())
                }

            val groupId =
                db.insertOrThrow(
                    "membership_groups",
                    null,
                    groupValues
                )

            memberIds.forEach { id ->

                val membership =
                    ContentValues().apply {

                        put("member_id", id)
                        put("plan_id", planId)
                        put("start_date", start)
                        put("end_date", end)
                        put("plan_amount", price)
                        put("discount_type", type)
                        put("discount_value", discountValue)
                        put("discount_amount", discount)
                        put("total_amount", total)

                        // Shared payment stored once
                        // at group level.
                        put("paid_amount", 0.0)
                        put("balance_amount", 0.0)

                        put(
                            "status",
                            if (balance <= 0.005)
                                "Active"
                            else
                                "Pending"
                        )

                        put("frozen", 0)
                        put("is_trial", 0)
                        put("group_id", groupId)
                        put("created_at", now())
                    }

                db.insertOrThrow(
                    "memberships",
                    null,
                    membership
                )
            }

            if (paid > 0.0) {

                val firstMembershipId =
                    db.rawQuery(
                        """
                        SELECT id
                        FROM memberships
                        WHERE group_id=?
                        ORDER BY id
                        LIMIT 1
                        """,
                        arrayOf(groupId.toString())
                    )

                firstMembershipId.use {

                    if (it.moveToFirst()) {

                        val payment =
                            ContentValues().apply {

                                put(
                                    "member_id",
                                    memberIds.first()
                                )

                                put(
                                    "membership_id",
                                    it.getLong(0)
                                )

                                put("amount", paid)
                                put(
                                    "method",
                                    paymentMethod
                                )
                                put("reference", "")
                                put(
                                    "notes",
                                    "Shared group membership payment - $groupCode"
                                )
                                put(
                                    "paid_at",
                                    now()
                                )
                            }

                        db.insertOrThrow(
                            "payments",
                            null,
                            payment
                        )
                    }
                }
            }

            db.setTransactionSuccessful()

        } finally {
            db.endTransaction()
        }

        return balance
    }

    fun addPayment(
        memberCode: String,
        amount: Double,
        method: String,
        reference: String,
        notes: String
    ) {

        if (amount <= 0) {
            throw IllegalArgumentException(
                "Payment amount must be greater than zero."
            )
        }

        val db = writableDatabase

        val memberId =
            findMemberId(memberCode)
                ?: throw IllegalArgumentException(
                    "Member not found."
                )

        val membership = db.rawQuery(
            """
            SELECT id,total_amount,paid_amount,balance_amount
            FROM memberships
            WHERE member_id=?
            ORDER BY id DESC
            LIMIT 1
            """,
            arrayOf(memberId.toString())
        )

        membership.use {

            if (!it.moveToFirst()) {
                throw IllegalArgumentException(
                    "No membership found."
                )
            }

            val membershipId = it.getLong(0)
            val total = it.getDouble(1)
            val oldPaid = it.getDouble(2)
            val oldBalance = it.getDouble(3)

            val actual =
                amount.coerceAtMost(
                    oldBalance.coerceAtLeast(0.0)
                )

            if (actual <= 0.0) {
                throw IllegalArgumentException(
                    "There is no outstanding balance."
                )
            }

            val payment =
                ContentValues().apply {

                    put("member_id", memberId)
                    put("membership_id", membershipId)
                    put("amount", actual)
                    put("method", method)
                    put("reference", reference)
                    put("notes", notes)
                    put("paid_at", now())
                }

            db.insertOrThrow(
                "payments",
                null,
                payment
            )

            val newPaid =
                (oldPaid + actual)
                    .coerceAtMost(total)

            val newBalance =
                (total - newPaid)
                    .coerceAtLeast(0.0)

            val status =
                if (newBalance <= 0.005)
                    "Active"
                else
                    "Pending"

            val update =
                ContentValues().apply {

                    put("paid_amount", newPaid)
                    put(
                        "balance_amount",
                        newBalance
                    )
                    put("status", status)
                }

            db.update(
                "memberships",
                update,
                "id=?",
                arrayOf(membershipId.toString())
            )
        }
    }

    fun ensureExpiredMembers() {

        val db = writableDatabase
        val todayDate = today()
        db.execSQL(

            """
                UPDATE memberships
                SET status = 'EXPIRED'
                WHERE is_trial = 0
                  AND frozen = 0
                  AND date(end_date) < date(?)
                  AND id IN(
                  SELECT MAX(ms2.id)
                  FROM memberships ms2
                  GROUP BY ms2.member_id
                  )
                  AND UPPER(status) != 'EXPIRED'
            """.trimIndent(),
            arrayOf(todayDate)
        )
    }


    fun checkIn(
        memberCode: String
    ): Boolean {

        val db = writableDatabase

        val memberId =
            findMemberId(memberCode)
                ?: throw IllegalArgumentException(
                    "Member not found."
                )

        val active = db.rawQuery(
            """
            SELECT ms.id
            FROM memberships ms
            WHERE ms.member_id=?
              AND ms.frozen=0
              AND ms.status IN ('Active','Trial','Pending')
              AND date(ms.start_date)<=date('now')
              AND date(ms.end_date)>=date('now')
            ORDER BY ms.id DESC
            LIMIT 1
            """,
            arrayOf(memberId.toString())
        )

        active.use {

            if (!it.moveToFirst()) {
                throw IllegalArgumentException(
                    "Member does not have an active membership."
                )
            }
        }

        val open = db.rawQuery(
            """
            SELECT id
            FROM attendance
            WHERE member_id=?
              AND check_out IS NULL
            LIMIT 1
            """,
            arrayOf(memberId.toString())
        )

        open.use {

            if (it.moveToFirst()) {
                return false
            }
        }

        val values =
            ContentValues().apply {

                put("member_id", memberId)
                put("check_in", now())
                put("workout_hours", 0.0)
                put("source", "Manual")
            }

        db.insertOrThrow(
            "attendance",
            null,
            values
        )

        return true
    }

    fun checkOut(
        memberCode: String
    ): Boolean {

        val db = writableDatabase

        val memberId =
            findMemberId(memberCode)
                ?: throw IllegalArgumentException(
                    "Member not found."
                )

        val c = db.rawQuery(
            """
            SELECT id, check_in
            FROM attendance
            WHERE member_id=?
              AND check_out IS NULL
            ORDER BY id DESC
            LIMIT 1
            """,
            arrayOf(memberId.toString())
        )

        c.use {

            if (!it.moveToFirst()) {
                return false
            }

            val id = it.getLong(0)
            val checkIn = it.getString(1)

            val checkOut = now()

            val format = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )

            val checkInDate =
                try {
                    format.parse(checkIn)
                } catch (_: Exception) {
                    null
                }

            val checkOutDate =
                try {
                    format.parse(checkOut)
                } catch (_: Exception) {
                    null
                }

            val workoutHours =
                if (
                    checkInDate != null &&
                    checkOutDate != null
                ) {

                    (
                            checkOutDate.time -
                                    checkInDate.time
                            ).toDouble() /
                            (1000.0 * 60.0 * 60.0)

                } else {
                    0.0
                }

            val safeWorkoutHours =
                workoutHours.coerceAtLeast(0.0)

            val values =
                ContentValues().apply {

                    put(
                        "check_out",
                        checkOut
                    )

                    put(
                        "workout_hours",
                        safeWorkoutHours
                    )
                }

            db.update(
                "attendance",
                values,
                "id=?",
                arrayOf(id.toString())
            )

            return true
        }
    }

    fun autoClockOutAfter6Hours() {

        val db = writableDatabase

        db.execSQL(
            """
            UPDATE attendance
            SET
                check_out = datetime(check_in, '+6 hours'),
                workout_hours = 6.00,
                source = 'AutoClockOut'
            WHERE check_out IS NULL
              AND datetime(check_in)
                <= datetime('now', 'localtime', '-6 hours')
            """
        )
    }

    fun getAttendance(): List<Array<String>> {

        val rows = mutableListOf<Array<String>>()

        val c = readableDatabase.rawQuery(
            """
            SELECT
                a.id,
                m.member_code,
                m.name,
                a.check_in,
                COALESCE(a.check_out,'OPEN'),
                COALESCE(a.workout_hours,0),
                a.source
            FROM attendance a
            JOIN members m
                ON m.id=a.member_id
            ORDER BY a.id DESC
            LIMIT 500
            """,
            null
        )

        c.use {

            while (it.moveToNext()) {

                rows.add(
                    arrayOf(
                        it.getString(0),
                        it.getString(1),
                        it.getString(2),
                        it.getString(3),
                        it.getString(4),
                        it.getString(5),
                        it.getString(6)
                    )
                )
            }
        }

        return rows
    }

    fun addTrainer(
        name: String,
        phone: String,
        email: String,
        specialization: String
    ) {

        val values =
            ContentValues().apply {

                put("name", name.trim())
                put("phone", phone.trim())
                put("email", email.trim())
                put(
                    "specialization",
                    specialization.trim()
                )
                put("active", 1)
            }

        writableDatabase.insertOrThrow(
            "trainers",
            null,
            values
        )
    }

    fun getTrainers(): List<Array<String>> {

        val rows = mutableListOf<Array<String>>()

        val c = readableDatabase.rawQuery(
            """
            SELECT
                id,
                name,
                phone,
                email,
                specialization,
                active
            FROM trainers
            ORDER BY id DESC
            """,
            null
        )

        c.use {

            while (it.moveToNext()) {

                rows.add(
                    arrayOf(
                        it.getString(0),
                        it.getString(1),
                        it.getString(2),
                        it.getString(3),
                        it.getString(4),
                        it.getString(5)
                    )
                )
            }
        }

        return rows
    }

    fun addExpense(
        category: String,
        description: String,
        amount: Double,
        date: String,
        notes: String
    ) {

        if (amount <= 0) {
            throw IllegalArgumentException(
                "Expense amount must be greater than zero."
            )
        }

        val values =
            ContentValues().apply {

                put("category", category.trim())
                put(
                    "description",
                    description.trim()
                )
                put("amount", amount)
                put("expense_date", date)
                put("notes", notes.trim())
            }

        writableDatabase.insertOrThrow(
            "expenses",
            null,
            values
        )
    }

    fun getExpenses(): List<Array<String>> {

        val rows = mutableListOf<Array<String>>()

        val c = readableDatabase.rawQuery(
            """
            SELECT
                id,
                category,
                description,
                amount,
                expense_date,
                notes
            FROM expenses
            ORDER BY id DESC
            """,
            null
        )

        c.use {

            while (it.moveToNext()) {

                rows.add(
                    arrayOf(
                        it.getString(0),
                        it.getString(1),
                        it.getString(2),
                        it.getString(3),
                        it.getString(4),
                        it.getString(5)
                    )
                )
            }
        }

        return rows
    }

    fun addUser(
        username: String,
        password: String,
        role: String
    ) {

        if (username.trim().isBlank()) {
            throw IllegalArgumentException(
                "Username is required."
            )
        }

        if (password.isBlank()) {
            throw IllegalArgumentException(
                "Password is required."
            )
        }

        val values =
            ContentValues().apply {

                put(
                    "username",
                    username.trim()
                )
                put("password", password)
                put("role", role)
                put("active", 1)
                put("created_at", now())
            }

        writableDatabase.insertOrThrow(
            "users",
            null,
            values
        )
    }

    fun getUsers(): List<Array<String>> {

        val rows = mutableListOf<Array<String>>()

        val c = readableDatabase.rawQuery(
            """
            SELECT
                id,
                username,
                role,
                active
            FROM users
            ORDER BY id ASC
            """,
            null
        )

        c.use {

            while (it.moveToNext()) {

                rows.add(
                    arrayOf(
                        it.getString(0),
                        it.getString(1),
                        it.getString(2),
                        it.getString(3)
                    )
                )
            }
        }

        return rows
    }

    fun toggleUser(id: Long) {

        val values = ContentValues()

        val current =
            scalarInt(
                """
                SELECT active
                FROM users
                WHERE id=$id
                  AND username<>'admin'
                LIMIT 1
                """
            )

        values.put(
            "active",
            if (current == 1) 0 else 1
        )

        writableDatabase.update(
            "users",
            values,
            "id=? AND username<>'admin'",
            arrayOf(id.toString())
        )
    }

    fun backupSqlite(): String {

        writableDatabase.execSQL(
            "PRAGMA wal_checkpoint(FULL)"
        )

        close()

        return File(
            contextFilesDir(),
            DB_NAME
        ).absolutePath
    }

    fun isDatabaseReady(): Boolean =
        getGymSettings().isNotEmpty()

    private fun findMemberId(
        memberCode: String
    ): Long? {

        val c = readableDatabase.rawQuery(
            """
            SELECT id
            FROM members
            WHERE UPPER(member_code)=UPPER(?)
            LIMIT 1
            """,
            arrayOf(memberCode.trim())
        )

        c.use {

            return if (it.moveToFirst()) {
                it.getLong(0)
            } else {
                null
            }
        }
    }

    private fun contextFilesDir(): File {
        return appContext.getDatabasePath(DB_NAME)
    }

    private fun now(): String =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    private fun today(): String =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())

    private fun addMonths(
        date: String,
        months: Int
    ): String {

        val input =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        val d =
            input.parse(date) ?: Date()

        val cal =
            java.util.Calendar.getInstance().apply {
                time = d
                add(
                    java.util.Calendar.MONTH,
                    months
                )
                add(
                    java.util.Calendar.DAY_OF_MONTH,
                    -1
                )
            }

        return input.format(cal.time)
    }
}