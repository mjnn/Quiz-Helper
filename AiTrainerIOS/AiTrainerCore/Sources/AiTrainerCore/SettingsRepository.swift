import Foundation

public final class SettingsRepository {
    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public func loadDrawSettings() -> PracticeDrawSettings {
        guard let data = defaults.data(forKey: AppConfig.keyDrawSettings),
              let settings = try? JSONDecoder().decode(PracticeDrawSettings.self, from: data) else {
            return PracticeDrawSettings()
        }
        return settings.normalized()
    }

    public func saveDrawSettings(_ settings: PracticeDrawSettings) {
        let normalized = settings.normalized()
        if let data = try? JSONEncoder().encode(normalized) {
            defaults.set(data, forKey: AppConfig.keyDrawSettings)
        }
    }
}
