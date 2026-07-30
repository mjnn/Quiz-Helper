import SwiftUI

enum AppTheme {
    static let paper = Color(red: 0.965, green: 0.961, blue: 0.949)
    static let surfaceWhite = Color.white
    static let inkPrimary = Color(red: 0.102, green: 0.102, blue: 0.102)
    static let inkSecondary = Color(red: 0.388, green: 0.388, blue: 0.400)
    static let inkTertiary = Color(red: 0.596, green: 0.596, blue: 0.616)
    static let accent = Color(red: 0.114, green: 0.306, blue: 0.847)
    static let accentSoft = Color(red: 0.937, green: 0.965, blue: 1.0)
    static let success = Color(red: 0.020, green: 0.588, blue: 0.412)
    static let warning = Color(red: 0.851, green: 0.467, blue: 0.024)
    static let danger = Color(red: 0.863, green: 0.149, blue: 0.149)
    static let hairline = Color(red: 0.906, green: 0.898, blue: 0.922)
}

struct ElevatedCard<Content: View>: View {
    private let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12, content: content)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppTheme.surfaceWhite)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: Color.black.opacity(0.04), radius: 8, y: 2)
    }
}

struct PrimaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(AppTheme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}

struct SecondaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .foregroundStyle(AppTheme.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(AppTheme.accentSoft)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}
