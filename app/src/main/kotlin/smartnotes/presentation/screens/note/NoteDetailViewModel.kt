package smartnotes.presentation.screens.note

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import smartnotes.domain.models.Note
import smartnotes.domain.values.NotePriority
import smartnotes.presentation.common.Response
import smartnotes.presentation.usecase.NoteUseCase
import smartnotes.presentation.usecase.UserUseCase
import smartnotes.utils.kotlin.MementoCaretaker
import javax.inject.Inject

/**
 * ViewModel экрана создания и редактирования заметки.
 *
 * @property disposables Контейнер подписок.
 * @property _liveSaveNote Содержит результат операции сохранения заметки.
 * @property _liveDeleteNote Содержит результат операции удаления заметки.
 * @property _liveExportToFile Содержит результат операции експорта заметки в файл.
 *
 * @property note Текущее состояние заметки.
 * @property liveSaveNote Предоставляет публичный интерфейс [_liveSaveNote].
 * @property liveDeleteNote Предоставляет публичный интерфейс [_liveDeleteNote].
 * @property liveExportToFile Предоставляет публичный интерфейс [_liveExportToFile].
 *
 * @author Pavel Annin (https://github.com/anninpavel).
 */
class NoteDetailViewModel(
    private val notes: NoteUseCase,
    private val userUseCase: UserUseCase,
    private val editor: NoteEditor,
    private val caretaker: MementoCaretaker<Note>
) : ViewModel() {

    @Inject constructor(notes: NoteUseCase, userUseCase: UserUseCase) : this(notes, userUseCase, NoteEditorImpl())

    constructor(notes: NoteUseCase, userUseCase: UserUseCase, editor: NoteEditorImpl)
            : this(notes, userUseCase, editor, MementoCaretaker(editor))

    private val disposables = CompositeDisposable()
    private val _liveSaveNote = MutableLiveData<Response<Unit>>()
    private val _liveDeleteNote = MutableLiveData<Response<Unit>>()
    private val _liveExportToFile = MutableLiveData<Response<Unit>>()

    val note: Note
        get() = editor.note

    val liveSaveNote: LiveData<Response<Unit>>
        get() = _liveSaveNote

    val liveDeleteNote: LiveData<Response<Unit>>
        get() = _liveDeleteNote

    val liveExportToFile: LiveData<Response<Unit>>
        get() = _liveExportToFile

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }

    /** Изменяет заголовок заметки. */
    fun editTitle(value: CharSequence?) {
        editor.title(value = value?.toString() ?: "")
        caretaker.save()
    }

    /** Изменяет текст заметки. */
    fun editText(value: CharSequence?) {
        editor.text(value = value?.toString() ?: "")
        caretaker.save()
    }

    /** Изменяет приоритет заметки. */
    fun editPriority(value: NotePriority) {
        editor.priority(value)
        caretaker.save()
    }

    /** Изменяет полное состояние заметки. */
    fun editNote(value: Note) {
        editor.note = value
    }

    /**
     * Сохраняет изменения в заметке.
     * Результат оперции передается [_liveSaveNote].
     */
    fun save() {
        notes.save(editor.note)
            .doOnSubscribe { _liveSaveNote.value = Response.loading() }
            .subscribe(
                { _liveSaveNote.value = Response.success(value = Unit) },
                { _liveSaveNote.value = Response.failure(error = it) }
            ).addTo(disposables)
    }

    /**
     * Удаляет заметку.
     * Результат оперции передается [_liveDeleteNote].
     */
    fun delete() {
        notes.delete(editor.note)
            .doOnSubscribe { _liveDeleteNote.value = Response.loading() }
            .subscribe(
                { _liveDeleteNote.value = Response.success(value = Unit) },
                { _liveDeleteNote.value = Response.failure(error = it) }
            ).addTo(disposables)
    }

    /**
     * Экспортирует заметку в файл.
     * Результат оперции передается [_liveExportToFile].
     *
     * @param desiredDirectory Желаемая директория для экспорта (опционально).
     */
    fun exportToFile(desiredDirectory: DocumentFile? = null) {
        val outputDirectory = userUseCase.exportDirectory(desiredDirectory)
        notes.exportToFile(editor.note, outputDirectory)
            .doOnSubscribe { _liveExportToFile.value = Response.loading() }
            .subscribe(
                { _liveExportToFile.value = Response.success(value = Unit) },
                { _liveExportToFile.value = Response.failure(error = it) }
            ).addTo(disposables)
    }
}
