import Foundation

public enum AppConfig {
    public static let author = "马老师"
    public static let versionLabel = "V1.15"
    public static let qbankVersion = "20260726_604"
    public static let defaultSessionLimit = 50
    public static let minSessionLimit = 5
    public static let maxSessionLimit = 100
    public static let skip = "__SKIP__"
    public static let srsMaxNewPerSession = 20
    public static let srsStorageVersion = "srs_v2"
    public static let importedFileName = "imported_questions.json"

    public static let prefsName = "ai_trainer_store"
    public static let keyMemory = "ai_train_memory_v1"
    public static let keyWrongLedger = "ai_train_wrong_ledger_v1"
    public static let keySession = "ai_train_session_v1"
    public static let keyDrawSettings = "ai_train_draw_settings_v1"
    public static let keyInit = "ai_train_init_v1"
    public static let keyQbankVersion = "ai_train_qbank_version_v1"
    public static let keySrsVersion = "ai_train_srs_version_v1"
}
