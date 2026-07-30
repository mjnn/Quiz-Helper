import Foundation

public enum QuestionLogic {
    public static func optionLetter(_ opt: String) -> String {
        String(opt.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1))
    }

    public static func optionText(_ opt: String) -> String {
        opt.replacingOccurrences(of: #"^[A-D][.．、\s]+"#, with: "", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    public static func isCorrect(_ q: Question, userVal: String?) -> Bool {
        guard let userVal, !userVal.isEmpty, userVal != AppConfig.skip else { return false }
        if q.type == "判断" { return userVal == q.answer }
        return optionLetter(userVal) == q.answer
    }

    public static func correctText(_ q: Question) -> String {
        if q.type == "判断" { return q.answer }
        return q.options.first { optionLetter($0) == q.answer } ?? ""
    }

    public static func optionExplKey(_ q: Question, opt: String) -> String {
        q.type == "判断" ? judgeOptionLabel(opt) : optionLetter(opt)
    }

    public static func optionExplFor(_ q: Question, opt: String) -> String? {
        let key = optionExplKey(q, opt: opt)
        guard let text = q.optionExpls?[key]?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty else {
            return nil
        }
        return text
    }

    public static func hasExplanation(_ q: Question) -> Bool {
        if !q.expl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return true }
        if let answerExpl = q.answerExpl, !answerExpl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return true }
        return q.optionExpls?.values.contains { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty } ?? false
    }

    private static func judgeOptionLabel(_ opt: String) -> String {
        let trimmed = opt.trimmingCharacters(in: .whitespacesAndNewlines)
        if opt.contains("正确") || trimmed == "对" { return "正确" }
        if opt.contains("错误") || trimmed == "错" { return "错误" }
        return trimmed
    }

    public static func shuffle<T>(_ list: [T]) -> [T] {
        var a = list
        guard a.count > 1 else { return a }
        for i in stride(from: a.count - 1, through: 1, by: -1) {
            let j = Int.random(in: 0...i)
            a.swapAt(i, j)
        }
        return a
    }
}
