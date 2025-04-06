# NotesService

## Notes

Notes sections of this service has the following actions:

- Create Note (note format)
- Get Note (by providing the note ID)
- List Notes (list all notes for a specific user by providing user_id, page, and page_size)
- Update Note (if the user wants to update a note, they should provide the updated note to this endpoint)
- Delete Note (delete a specific note by providing the note ID and user_id)

Note format:

```proto
message Note {
  string id = 1;
  string user_id = 2;
  string title = 3;
  string content = 4;
  google.protobuf.Timestamp date = 5;
}
```

Actions for notes:

```proto
service NoteService {
  rpc CreateNote(CreateNoteRequest) returns (Note);
  rpc GetNote(GetNoteRequest) returns (Note);
  rpc ListNotes(ListNotesRequest) returns (ListNotesResponse);
  rpc UpdateNote(UpdateNoteRequest) returns (Note);
  rpc DeleteNote(DeleteNoteRequest) returns (google.protobuf.Empty);
}

message CreateNoteRequest {
  Note note = 1;
}

message GetNoteRequest {
  string id = 1;
}

message ListNotesRequest {
  string user_id = 1;
  int32 page = 2;
  int32 page_size = 3;
}

message ListNotesResponse {
  repeated Note notes = 1;
}

message UpdateNoteRequest {
  Note note = 1;
}

message DeleteNoteRequest {
  string id = 1;
  string user_id = 2;
}
```

Таблица заметок

```sql
create table notes (
    id varchar(36) PRIMary key,
    userid varchar(255) not null,
    title text not null,
    content text not null,
    date varchar(255) not null
);
```