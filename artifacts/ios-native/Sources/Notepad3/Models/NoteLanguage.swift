import Foundation

enum NoteLanguage: String, Codable, CaseIterable {
    case plain = "Plain"
    case markdown = "Markdown"
    case assembly = "Assembly"
    case javaScript = "JavaScript"
    case kotlin = "Kotlin"
    case swift = "Swift"
    case python = "Python"
    case cPlusPlus = "C++"
    case java = "Java"
    case cSharp = "C#"
    case go = "Go"
    case rust = "Rust"
    case dart = "Dart"
    case php = "PHP"
    case ruby = "Ruby"
    case shell = "Shell"
    case powerShell = "PowerShell"
    case sql = "SQL"
    case yaml = "YAML"
    case toml = "TOML"
    case ini = "INI"
    case dockerfile = "Dockerfile"
    case html = "HTML"
    case css = "CSS"
    case xml = "XML"
    case web = "Web"
    case json = "JSON"

    static let selectableLanguages: [NoteLanguage] = [
        .plain,
        .markdown,
        .json,
        .html,
        .css,
        .web,
        .javaScript,
        .kotlin,
        .swift,
        .python,
        .cPlusPlus,
        .java,
        .cSharp,
        .go,
        .rust,
        .dart,
        .php,
        .ruby,
        .shell,
        .powerShell,
        .sql,
        .yaml,
        .toml,
        .ini,
        .dockerfile,
        .xml,
        .assembly,
    ]

    static func detect(fromFileName name: String) -> NoteLanguage {
        let lower = name.lowercased().replacingOccurrences(of: "\\", with: "/")
        let base = (lower as NSString).lastPathComponent
        if base == "dockerfile" || base == "containerfile" || base.hasPrefix("dockerfile.") || base.hasPrefix("containerfile.") || base.hasSuffix(".dockerfile") { return .dockerfile }
        if ["gemfile", "rakefile"].contains(base) || base.range(of: #"\.(rb|rake|gemspec)$"#, options: .regularExpression) != nil { return .ruby }
        if [".bashrc", ".zshrc", ".profile"].contains(base) || base.range(of: #"\.(sh|bash|zsh|fish|ksh)$"#, options: .regularExpression) != nil { return .shell }
        if [".editorconfig", ".gitconfig"].contains(base) || base.range(of: #"\.(ini|cfg|conf)$"#, options: .regularExpression) != nil { return .ini }
        if base.range(of: #"\.(asm|s|nasm|masm|inc)$"#, options: .regularExpression) != nil { return .assembly }
        if base.range(of: #"\.(md|markdown)$"#, options: .regularExpression) != nil { return .markdown }
        if base.range(of: #"\.(js|jsx|ts|tsx|mjs|cjs)$"#, options: .regularExpression) != nil { return .javaScript }
        if base.range(of: #"\.(kt|kts)$"#, options: .regularExpression) != nil { return .kotlin }
        if base.range(of: #"\.swift$"#, options: .regularExpression) != nil { return .swift }
        if base.range(of: #"\.java$"#, options: .regularExpression) != nil { return .java }
        if base.range(of: #"\.(cs|csx)$"#, options: .regularExpression) != nil { return .cSharp }
        if base.range(of: #"\.go$"#, options: .regularExpression) != nil { return .go }
        if base.range(of: #"\.rs$"#, options: .regularExpression) != nil { return .rust }
        if base.range(of: #"\.dart$"#, options: .regularExpression) != nil { return .dart }
        if base.range(of: #"\.(php|phtml|php3|php4|php5)$"#, options: .regularExpression) != nil { return .php }
        if base.range(of: #"\.(ps1|psm1|psd1)$"#, options: .regularExpression) != nil { return .powerShell }
        if base.range(of: #"\.sql$"#, options: .regularExpression) != nil { return .sql }
        if base.range(of: #"\.(yml|yaml)$"#, options: .regularExpression) != nil { return .yaml }
        if base.range(of: #"\.toml$"#, options: .regularExpression) != nil { return .toml }
        if base.range(of: #"\.(py|pyw)$"#, options: .regularExpression) != nil { return .python }
        if base.range(of: #"\.(c|cc|cpp|cxx|h|hh|hpp|hxx)$"#, options: .regularExpression) != nil { return .cPlusPlus }
        if base.range(of: #"\.(html|htm)$"#, options: .regularExpression) != nil { return .html }
        if base.range(of: #"\.css$"#, options: .regularExpression) != nil { return .css }
        if base.range(of: #"\.(xml|svg)$"#, options: .regularExpression) != nil { return .xml }
        if base.range(of: #"\.(json|jsonc)$"#, options: .regularExpression) != nil { return .json }
        return .plain
    }

    /// Keywords that trigger keyword styling. Empty for languages with no keyword set
    /// (plain, markdown).
    var keywords: Set<String> {
        switch self {
        case .assembly:
            return Self.assemblyOps
        case .javaScript:
            return Self.commonKeywords.union(Self.javaScriptKeywords)
        case .kotlin:
            return Self.commonKeywords.union(Self.cLikeKeywords).union(Self.kotlinKeywords)
        case .swift:
            return Self.commonKeywords.union(Self.swiftKeywords)
        case .python:
            return Self.commonKeywords.union(Self.pythonKeywords)
        case .cPlusPlus:
            return Self.commonKeywords.union(Self.cLikeKeywords).union(Self.cppKeywords)
        case .java:
            return Self.commonKeywords.union(Self.cLikeKeywords).union(Self.javaKeywords)
        case .cSharp:
            return Self.commonKeywords.union(Self.cLikeKeywords).union(Self.cSharpKeywords)
        case .go:
            return Self.commonKeywords.union(Self.goKeywords)
        case .rust:
            return Self.commonKeywords.union(Self.rustKeywords)
        case .dart:
            return Self.commonKeywords.union(Self.cLikeKeywords).union(Self.dartKeywords)
        case .php:
            return Self.commonKeywords.union(Self.phpKeywords)
        case .ruby:
            return Self.commonKeywords.union(Self.rubyKeywords)
        case .shell:
            return Self.shellKeywords
        case .powerShell:
            return Self.commonKeywords.union(Self.powerShellKeywords)
        case .sql:
            return Self.sqlKeywords
        case .yaml, .toml, .ini, .dockerfile:
            return Self.configKeywords
        case .html, .css, .xml, .web, .json:
            return Self.commonKeywords.union(Self.markupKeywords)
        default:
            return []
        }
    }

    var registers: Set<String> {
        self == .assembly ? Self.assemblyRegisters : []
    }

    var isKeywordCaseInsensitive: Bool {
        switch self {
        case .assembly, .shell, .powerShell, .sql, .yaml, .toml, .ini, .dockerfile, .html, .css, .xml, .web:
            return true
        default:
            return false
        }
    }

    /// Comment-prefix patterns (anchored to start-of-substring) used by the highlighter.
    var commentPrefixes: [String] {
        switch self {
        case .assembly: return [";"]
        case .python, .ruby, .shell, .powerShell, .yaml, .toml, .dockerfile: return ["#"]
        case .ini: return [";", "#"]
        case .sql: return ["--"]
        case .html, .xml: return ["<!--"]
        case .javaScript, .kotlin, .swift, .cPlusPlus, .java, .cSharp, .go, .rust, .dart, .php, .web, .json: return ["//"]
        default: return []
        }
    }

    var supportsBlockComments: Bool {
        switch self {
        case .javaScript, .kotlin, .swift, .cPlusPlus, .java, .cSharp, .go, .rust, .dart, .php, .sql, .css, .web, .json:
            return true
        default:
            return false
        }
    }

    private static let assemblyOps: Set<String> = Set("""
        mov lea push pop call ret jmp je jne jz jnz ja jae jb jbe jl jle jg jge \
        cmp test add sub inc dec mul imul div idiv and or xor not shl shr sal sar \
        rol ror nop int syscall sysenter leave enter rep repe repne stosb stosw \
        stosd movsb movsw movsd lodsb lodsw lodsd scasb scasw scasd cmpsb cmpsw \
        cmpsd db dw dd dq section global extern bits org equ
        """.split(separator: " ").map(String.init))

    private static let assemblyRegisters: Set<String> = Set("""
        al ah ax eax rax bl bh bx ebx rbx cl ch cx ecx rcx dl dh dx edx rdx \
        si esi rsi di edi rdi sp esp rsp bp ebp rbp r8 r9 r10 r11 r12 r13 r14 r15 \
        r8d r9d r10d r11d r12d r13d r14d r15d xmm0 xmm1 xmm2 xmm3 xmm4 xmm5 xmm6 xmm7 \
        ymm0 ymm1 ymm2 ymm3 ymm4 ymm5 ymm6 ymm7 cs ds es fs gs ss
        """.split(separator: " ").map(String.init))

    private static let commonKeywords: Set<String> = Set("""
        if else for while do switch case default break continue return throw try catch finally \
        true false null nil none yes no on off
        """.split(separator: " ").map(String.init))

    private static let cLikeKeywords: Set<String> = Set("""
        class interface enum struct public private protected static final abstract override \
        void int long short byte char float double bool boolean string new this super extends \
        implements import package namespace using const var let
        """.split(separator: " ").map(String.init))

    private static let javaScriptKeywords: Set<String> = Set("""
        const let var function class import export from async await typeof undefined yield \
        interface type extends implements readonly keyof declare module require
        """.split(separator: " ").map(String.init))

    private static let kotlinKeywords: Set<String> = Set("""
        fun val var object data sealed companion suspend inline reified when is in as null \
        package import open internal lateinit by get set
        """.split(separator: " ").map(String.init))

    private static let swiftKeywords: Set<String> = Set("""
        func let var class struct enum protocol extension guard defer inout throws async await \
        actor associatedtype where self Self import public private fileprivate internal open
        """.split(separator: " ").map(String.init))

    private static let pythonKeywords: Set<String> = Set("""
        def lambda pass in is and or not from import as with yield global nonlocal elif except \
        raise assert del True False None async await class
        """.split(separator: " ").map(String.init))

    private static let cppKeywords: Set<String> = Set("""
        template typename include define ifdef ifndef endif pragma auto constexpr noexcept nullptr \
        virtual friend operator unsigned signed size_t std
        """.split(separator: " ").map(String.init))

    private static let javaKeywords: Set<String> = Set("""
        record sealed permits synchronized volatile transient native strictfp throws instanceof
        """.split(separator: " ").map(String.init))

    private static let cSharpKeywords: Set<String> = Set("""
        var dynamic readonly ref out in params async await namespace using get set init partial \
        record sealed virtual override event delegate where yield nameof nullable
        """.split(separator: " ").map(String.init))

    private static let goKeywords: Set<String> = Set("""
        package import func defer go chan select range map interface struct type var const iota \
        fallthrough nil make new append cap close complex copy delete imag len panic print println real recover
        """.split(separator: " ").map(String.init))

    private static let rustKeywords: Set<String> = Set("""
        fn let mut pub crate mod use impl trait enum struct async await match if let loop move \
        ref self Self super unsafe where dyn const static type Some None Ok Err Result Option
        """.split(separator: " ").map(String.init))

    private static let dartKeywords: Set<String> = Set("""
        class mixin extension import export part async await yield late required final const var \
        dynamic typedef factory implements with library nullable true false null
        """.split(separator: " ").map(String.init))

    private static let phpKeywords: Set<String> = Set("""
        php echo function namespace use class trait interface extends implements public private \
        protected static final abstract var yield match fn array null true false
        """.split(separator: " ").map(String.init))

    private static let rubyKeywords: Set<String> = Set("""
        def end class module require include extend attr_reader attr_writer attr_accessor begin rescue \
        ensure elsif unless until yield self nil true false do
        """.split(separator: " ").map(String.init))

    private static let shellKeywords: Set<String> = Set("""
        if then else elif fi for while until do done case esac function select in export local \
        readonly unset shift source alias test true false
        """.split(separator: " ").map(String.init))

    private static let powerShellKeywords: Set<String> = Set("""
        function param process begin end if elseif else foreach for while do switch try catch finally \
        throw return break continue filter workflow class enum using namespace true false null
        """.split(separator: " ").map(String.init))

    private static let sqlKeywords: Set<String> = Set("""
        select from where join inner left right full outer on group by order having insert into update \
        delete create alter drop table view index primary foreign key references constraint values set \
        null not and or as distinct union all limit offset case when then else end
        """.split(separator: " ").map(String.init))

    private static let markupKeywords: Set<String> = Set("""
        html head body div span script style link meta title section article header footer nav main \
        form input button table tr td th ul ol li svg path rect circle viewBox xmlns
        """.split(separator: " ").map(String.init))

    private static let configKeywords: Set<String> = Set("""
        true false null yes no on off version services image build ports volumes environment command \
        from run copy add cmd entrypoint expose workdir user arg env label maintainer
        """.split(separator: " ").map(String.init))
}
