import UIKit

/// Applies syntax coloring to a `UITextView`'s underlying text storage by
/// mutating only foreground color attributes — the text content and user
/// cursor stay untouched. Re-run on every store-driven body change.
///
/// Tokenizer is deliberately simple: regex passes for comments, strings,
/// numbers, identifiers (classified as keyword/register via `NoteLanguage`'s
/// sets), function calls, operators, and decorators. Claimed ranges prevent
/// later passes from recoloring comment/string interiors.
///
/// Pass precedence (earlier claims win):
///   1. Block comments    — `/* ... */` multi-line, only when the language
///                           treats `//` as a line comment (JS / Web / JSON).
///   2. Line comments     — single-line to newline.
///   3. Template strings  — backtick-delimited, with backtick escape support.
///   4. Regular strings   — double- / single-quoted, same-line.
///   5. Numbers           — decimal + simple float + hex.
///   6. Function calls    — identifier immediately followed by `(`, unless
///                           the identifier matches a keyword (kept as keyword).
///                           Skipped for Assembly.
///   7. Keywords / registers — language-specific identifier sets.
///   8. Decorators        — `@identifier` lines in JavaScript.
///   9. Operators         — applied last, only on still-unclaimed positions.
enum SyntaxHighlighter {
    static func apply(to textView: UITextView, language: NoteLanguage, palette: Palette) {
        let storage = textView.textStorage
        let nsText = storage.string as NSString
        let fullRange = NSRange(location: 0, length: nsText.length)

        storage.beginEditing()
        storage.removeAttribute(.foregroundColor, range: fullRange)
        storage.addAttribute(.foregroundColor, value: palette.foreground, range: fullRange)

        for token in tokens(in: nsText, language: language) {
            storage.addAttribute(.foregroundColor, value: color(token.kind, in: palette), range: token.range)
        }
        storage.endEditing()
    }

    // MARK: - Tokenization

    private struct Token {
        enum Kind {
            case keyword, register, string, comment, number
            case templateString, functionCall, operatorSymbol, decorator, blockComment
        }
        let range: NSRange
        let kind: Kind
    }

    private static func tokens(in text: NSString, language: NoteLanguage) -> [Token] {
        let full = NSRange(location: 0, length: text.length)
        let source = text as String
        var out: [Token] = []
        var claimed = IndexSet()

        // Block comments are honored when the language uses `//` as a line
        // comment — that covers C-style languages plus CSS block comments.
        let supportsBlockComments = language.supportsBlockComments
        let isAssembly = language == .assembly
        let supportsAtRules = language == .javaScript || language == .python || language == .css

        // 1. Block comments — `/* ... */`, multi-line, non-greedy.
        if supportsBlockComments,
           let re = Regex.blockComment {
            re.enumerateMatches(in: source, range: full) { match, _, _ in
                guard let r = match?.range else { return }
                out.append(Token(range: r, kind: .blockComment))
                claim(r, in: &claimed)
            }
        }

        // 2. Line comments — claim from prefix to end of line.
        for prefix in language.commentPrefixes {
            let pattern = "\(NSRegularExpression.escapedPattern(for: prefix))[^\\n]*"
            guard let re = try? NSRegularExpression(pattern: pattern) else { continue }
            re.enumerateMatches(in: source, range: full) { match, _, _ in
                guard let r = match?.range, !isClaimed(r, in: claimed) else { return }
                out.append(Token(range: r, kind: .comment))
                claim(r, in: &claimed)
            }
        }

        // 3. Template strings — backtick-delimited, backtick-escape supported,
        // may span newlines (hence `[\\s\\S]`).
        if let re = Regex.templateString {
            re.enumerateMatches(in: source, range: full) { match, _, _ in
                guard let r = match?.range, !isClaimed(r, in: claimed) else { return }
                out.append(Token(range: r, kind: .templateString))
                claim(r, in: &claimed)
            }
        }

        // 4. Regular strings — double- and single-quoted, not spanning newlines.
        if let re = Regex.string {
            re.enumerateMatches(in: source, range: full) { match, _, _ in
                guard let r = match?.range, !isClaimed(r, in: claimed) else { return }
                out.append(Token(range: r, kind: .string))
                claim(r, in: &claimed)
            }
        }

        // 5. Numbers — decimal + optional fraction, or hex literals.
        if let re = Regex.number {
            re.enumerateMatches(in: source, range: full) { match, _, _ in
                guard let r = match?.range, !isClaimed(r, in: claimed) else { return }
                out.append(Token(range: r, kind: .number))
                claim(r, in: &claimed)
            }
        }

        // 6 + 7. Identifiers → keyword / register / function-call.
        // Keywords and registers beat function-call coloring so reserved words
        // stay as keywords even when followed by `(` (e.g. `if (...)`).
        let kw = language.keywords
        let reg = language.registers
        let caseInsensitiveKeywords = language.isKeywordCaseInsensitive
        if let re = Regex.identifier {
            re.enumerateMatches(in: source, range: full) { match, _, _ in
                guard let r = match?.range, !isClaimed(r, in: claimed) else { return }
                let word = text.substring(with: r)
                let lookup = caseInsensitiveKeywords ? word.lowercased() : word
                if kw.contains(lookup) {
                    out.append(Token(range: r, kind: .keyword))
                    claim(r, in: &claimed)
                } else if reg.contains(lookup) {
                    out.append(Token(range: r, kind: .register))
                    claim(r, in: &claimed)
                } else if !isAssembly, isFollowedByOpenParen(text: text, after: r) {
                    out.append(Token(range: r, kind: .functionCall))
                    claim(r, in: &claimed)
                }
            }
        }

        // 8. Decorators / at-rules — `@identifier` in JS/TS, Python, and CSS.
        if supportsAtRules,
           let re = Regex.decorator {
            re.enumerateMatches(in: source, range: full) { match, _, _ in
                guard let r = match?.range, !isClaimed(r, in: claimed) else { return }
                out.append(Token(range: r, kind: .decorator))
                claim(r, in: &claimed)
            }
        }

        // 9. Operators — applied last on still-unclaimed positions. Assembly
        // uses operators sparingly, so we scope this pass to code languages.
        if !isAssembly && language != .plain && language != .markdown {
            if let re = Regex.operatorSymbol {
                re.enumerateMatches(in: source, range: full) { match, _, _ in
                    guard let r = match?.range, !isClaimed(r, in: claimed) else { return }
                    out.append(Token(range: r, kind: .operatorSymbol))
                }
            }
        }

        return out
    }

    private enum Regex {
        static let blockComment = try? NSRegularExpression(pattern: "/\\*[\\s\\S]*?\\*/", options: [])
        static let templateString = try? NSRegularExpression(pattern: "`(?:\\\\.|[^`\\\\])*`", options: [])
        static let string = try? NSRegularExpression(pattern: #""(?:[^"\\\n]|\\.)*"|'(?:[^'\\\n]|\\.)*'"#)
        static let number = try? NSRegularExpression(pattern: "\\b(?:0x[0-9a-fA-F]+|\\d+(?:\\.\\d+)?)\\b")
        static let identifier = try? NSRegularExpression(pattern: "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        static let decorator = try? NSRegularExpression(pattern: "@[a-zA-Z_][a-zA-Z0-9_]*")
        static let operatorSymbol = try? NSRegularExpression(pattern: "[+\\-*/%=<>!&|\\^~?:]+")
    }

    /// Scan the immediate whitespace after `range` and return true if the next
    /// non-space character is `(`. Used to detect function-call sites.
    private static func isFollowedByOpenParen(text: NSString, after range: NSRange) -> Bool {
        var i = range.location + range.length
        while i < text.length {
            let ch = text.character(at: i)
            // space, tab — skip; anything else decides.
            if ch == 0x20 || ch == 0x09 { i += 1; continue }
            return ch == 0x28 // '('
        }
        return false
    }

    private static func isClaimed(_ r: NSRange, in claimed: IndexSet) -> Bool {
        guard r.length > 0 else { return false }
        let range = r.location..<(r.location + r.length)
        return claimed.rangeView.contains { $0.overlaps(range) }
    }

    private static func claim(_ r: NSRange, in claimed: inout IndexSet) {
        guard r.length > 0 else { return }
        claimed.insert(integersIn: r.location..<(r.location + r.length))
    }

    // MARK: - Colors

    /// Derive token colors from the palette so highlighting works in any theme.
    /// Light palettes get darker/saturated accents; dark palettes get brighter.
    private static func color(_ kind: Token.Kind, in palette: Palette) -> UIColor {
        switch kind {
        case .keyword:         return palette.primary
        case .register:        return palette.accent
        case .string:          return palette.success
        case .templateString:  return palette.success
        case .comment:         return palette.mutedForeground
        case .blockComment:    return palette.mutedForeground
        case .number:          return palette.accent
        case .functionCall:    return palette.accent
        case .operatorSymbol:  return palette.mutedForeground
        case .decorator:       return palette.accent
        }
    }
}
