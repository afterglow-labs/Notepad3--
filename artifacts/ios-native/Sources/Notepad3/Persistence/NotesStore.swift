import Foundation

/// Single source of truth for the user's notes. Backed by a JSON file in the
/// app's Documents directory. Observers are notified on every mutation;
/// observers register a closure keyed by an opaque token.
final class NotesStore {
    static let shared = NotesStore()

    private(set) var notes: [Note]
    private(set) var activeId: String

    private let url: URL
    private let persistenceQueue = DispatchQueue(label: "notepad3.notes.persistence", qos: .utility)
    private let draftPersistDelay: TimeInterval
    private var pendingPersist: DispatchWorkItem?
    private var hasPendingChanges = false
    private var observers: [UUID: () -> Void] = [:]

    init(fileManager: FileManager = .default, draftPersistDelay: TimeInterval = 0.45) {
        self.draftPersistDelay = draftPersistDelay
        let docs = (try? fileManager.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true))
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        self.url = docs.appendingPathComponent("notes-v1.json")

        if let data = try? Data(contentsOf: url),
           let snap = try? JSONDecoder.iso.decode(Snapshot.self, from: data),
           !snap.notes.isEmpty {
            self.notes = snap.notes
            self.activeId = snap.notes.contains(where: { $0.id == snap.activeId }) ? snap.activeId : snap.notes[0].id
        } else {
            let isBlank = Preferences.shared.starterContent == .blank
            let starter: Note = isBlank ? .blankWelcome : .welcome
            self.notes = [starter]
            self.activeId = starter.id
        }
    }

    var activeNote: Note {
        notes.first(where: { $0.id == activeId }) ?? notes[0]
    }

    @discardableResult
    func observe(_ block: @escaping () -> Void) -> UUID {
        let id = UUID()
        observers[id] = block
        return id
    }

    func unobserve(_ id: UUID) {
        observers.removeValue(forKey: id)
    }

    private func notify() {
        for block in observers.values { block() }
    }

    private func persist(_ snap: Snapshot) {
        if let data = try? JSONEncoder.iso.encode(snap) {
            try? data.write(to: url, options: .atomic)
        }
    }

    private func persistNow() {
        pendingPersist?.cancel()
        pendingPersist = nil
        let snap = Snapshot(notes: notes, activeId: activeId)
        persistenceQueue.sync { persist(snap) }
        hasPendingChanges = false
    }

    private func scheduleDraftPersist() {
        pendingPersist?.cancel()
        let snap = Snapshot(notes: notes, activeId: activeId)
        hasPendingChanges = true
        var item: DispatchWorkItem?
        item = DispatchWorkItem { [weak self, weak item] in
            guard let self, item?.isCancelled == false else { return }
            self.persist(snap)
            DispatchQueue.main.async { [weak self, weak item] in
                guard let self, let item, self.pendingPersist === item else { return }
                self.pendingPersist = nil
                self.hasPendingChanges = false
            }
        }
        pendingPersist = item
        persistenceQueue.asyncAfter(deadline: .now() + draftPersistDelay, execute: item!)
    }

    func flushPendingPersistence() {
        guard hasPendingChanges || pendingPersist != nil else { return }
        pendingPersist?.cancel()
        pendingPersist = nil
        let snap = Snapshot(notes: notes, activeId: activeId)
        persistenceQueue.sync { persist(snap) }
        hasPendingChanges = false
    }

    private func mutate(
        persistImmediately: Bool = true,
        notifyObservers: Bool = true,
        _ block: () -> Void
    ) {
        block()
        if persistImmediately {
            persistNow()
        } else {
            scheduleDraftPersist()
        }
        if notifyObservers {
            notify()
        }
    }

    func setActive(_ id: String) {
        guard notes.contains(where: { $0.id == id }), id != activeId else { return }
        mutate { activeId = id }
    }

    func updateActive(title: String? = nil, body: String? = nil, language: NoteLanguage? = nil) {
        updateActive(title: title, body: body, language: language, persistImmediately: true, notifyObservers: true)
    }

    func updateActiveDraft(body: String) {
        updateActive(title: nil, body: body, language: nil, persistImmediately: false, notifyObservers: false)
    }

    private func updateActive(
        title: String? = nil,
        body: String? = nil,
        language: NoteLanguage? = nil,
        persistImmediately: Bool,
        notifyObservers: Bool
    ) {
        guard let idx = notes.firstIndex(where: { $0.id == activeId }) else { return }
        var n = notes[idx]
        if let title { n.title = title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "untitled.txt" : title }
        if let body { n.body = body }
        if let language { n.language = language }
        n.updatedAt = Date()
        mutate(persistImmediately: persistImmediately, notifyObservers: notifyObservers) { notes[idx] = n }
    }

    @discardableResult
    func createBlank() -> Note {
        let note = Note(title: notes.nextUntitledName())
        mutate {
            notes.insert(note, at: 0)
            activeId = note.id
        }
        return note
    }

    @discardableResult
    func importNote(title: String, body: String, language: NoteLanguage = .plain) -> Note {
        let note = Note(title: title, body: body, language: language)
        mutate {
            notes.insert(note, at: 0)
            activeId = note.id
        }
        return note
    }

    @discardableResult
    func importFile(at sourceURL: URL) throws -> Note {
        let didStartAccess = sourceURL.startAccessingSecurityScopedResource()
        defer {
            if didStartAccess {
                sourceURL.stopAccessingSecurityScopedResource()
            }
        }

        let data = try Data(contentsOf: sourceURL)
        let text = String(data: data, encoding: .utf8)
            ?? String(data: data, encoding: .isoLatin1)
            ?? ""
        let title = sourceURL.lastPathComponent.isEmpty ? "untitled.txt" : sourceURL.lastPathComponent
        return importNote(title: title, body: text, language: NoteLanguage.detect(fromFileName: title))
    }

    func delete(id: String) {
        guard notes.count > 1 else {
            // Keep at least one note around — replace with a fresh blank.
            let blank = Note(title: "scratchpad.txt")
            mutate {
                notes = [blank]
                activeId = blank.id
            }
            return
        }
        mutate {
            notes.removeAll { $0.id == id }
            if activeId == id { activeId = notes[0].id }
        }
    }

    func closeOthers(keep id: String) {
        guard let keep = notes.first(where: { $0.id == id }) else { return }
        mutate {
            notes = [keep]
            activeId = keep.id
        }
    }

    func rename(id: String, title: String) {
        guard let idx = notes.firstIndex(where: { $0.id == id }) else { return }
        var n = notes[idx]
        n.title = title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "untitled.txt" : title
        n.updatedAt = Date()
        mutate { notes[idx] = n }
    }

    @discardableResult
    func duplicate(id: String) -> Note? {
        guard let src = notes.first(where: { $0.id == id }) else { return nil }
        var newTitle = src.title
        if let dot = newTitle.lastIndex(of: ".") {
            newTitle.insert(contentsOf: " copy", at: dot)
        } else {
            newTitle += " copy"
        }
        let copy = Note(title: newTitle, body: src.body, language: src.language)
        mutate {
            notes.insert(copy, at: 0)
            activeId = copy.id
        }
        return copy
    }

    private struct Snapshot: Codable {
        var notes: [Note]
        var activeId: String
    }
}

private extension JSONDecoder {
    static let iso: JSONDecoder = {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }()
}

private extension JSONEncoder {
    static let iso: JSONEncoder = {
        let e = JSONEncoder()
        e.dateEncodingStrategy = .iso8601
        return e
    }()
}
