# Room Database Migration - Testing and Verification Guide

## Overview
This document outlines the changes made to migrate the PencilNotes file management system from XML-based storage to Room database storage, and how to verify the implementation works correctly.

## Changes Made

### 1. Enhanced Room Database Structure
- **File**: `DrawRoom.kt`
- **Changes**: 
  - Added comprehensive CRUD operations to all DAOs
  - Updated Document entity to support nullable `folderId` for root documents
  - Incremented database version to 2
  - Added fallback destructive migration for development

### 2. Created Repository Layer
- **File**: `FileRepository.kt` (new)
- **Purpose**: Abstracts database operations and provides clean interface for ViewModels
- **Features**:
  - Handles folder and document CRUD operations
  - Proper error checking and validation
  - Support for hierarchical folder structure
  - Combined operations for UI (getItemsInFolder)

### 3. Updated FileExplorerViewModel
- **File**: `FileExplorerViewModel.kt`
- **Changes**:
  - Replaced XML-based persistence with Room database calls
  - Maintained same public interface for UI compatibility
  - Added database ID tracking for folders and documents
  - Optimistic UI updates for better user experience

### 4. Migration Utility
- **File**: `XMLToRoomMigration.kt` (new)
- **Purpose**: Automatically migrates existing XML-based file structures to database
- **Features**:
  - Parses existing XML files
  - Creates corresponding database entries
  - Backs up original XML files
  - Only runs if database is empty

### 5. MainActivity Update
- **File**: `MainActivity.kt`
- **Changes**: Updated ViewModel factory to pass Context instead of filesDir

## Testing Scenarios

### 1. Fresh Installation
**Expected Behavior:**
- App starts with empty file list
- Can create folders and documents
- All operations (create, rename, delete) work correctly
- Navigation between folders works
- Data persists between app restarts

**Testing Steps:**
1. Install app on fresh device/emulator
2. Create a folder named "Test Folder"
3. Enter the folder
4. Create a document named "Test Document"
5. Exit and restart the app
6. Verify folder and document are still there
7. Test rename and delete operations

### 2. Migration from XML
**Expected Behavior:**
- Existing XML-based file structure is migrated to database
- All folders and documents appear in the UI
- Original XML file is backed up
- Normal operations work after migration

**Testing Steps:**
1. Install previous version of app
2. Create some folders and documents using XML system
3. Update to new version with Room database
4. Verify all existing folders/documents appear
5. Check that XML backup file exists
6. Test that new operations work correctly

### 3. Database Operations
**Testing CRUD Operations:**

**Create:**
```kotlin
// Should create new folder/document in database
viewModel.createFile(FileType.FOLDER, "New Folder")
viewModel.createFile(FileType.FILE, "New Document")
```

**Read:**
```kotlin
// Should load items from database
viewModel.enterFolder("Test Folder") // Navigate to folder
// UI should show contents from database
```

**Update:**
```kotlin
// Should update name in database
viewModel.renameFile("Old Name", "New Name")
```

**Delete:**
```kotlin
// Should remove from database
viewModel.deleteFile("Test Document")
```

## Verification Points

### 1. Database Integrity
- Check that database file is created: `/data/data/com.studiomath.pencilnotes/databases/draw_database`
- Verify tables exist: `folders`, `documents`, `pages`, `resources`
- Check foreign key relationships are maintained

### 2. UI Compatibility
- All existing UI components should work without changes
- File list updates correctly after operations
- Navigation breadcrumbs work properly
- Create/rename/delete dialogs function normally

### 3. Performance
- Loading large folder structures should be fast
- Database queries should be efficient
- UI should remain responsive during operations

### 4. Error Handling
- Duplicate names should be prevented
- Invalid operations should fail gracefully
- Database errors should not crash the app

## Debugging

### Common Issues and Solutions

**Issue**: Files don't appear after migration
**Solution**: Check if XMLToRoomMigration ran successfully, verify XML file format

**Issue**: Database not created
**Solution**: Verify Room dependencies in build.gradle, check for compilation errors

**Issue**: UI not updating after operations
**Solution**: Ensure loadCurrentDirectory() is called after database operations

**Issue**: Migration runs every time
**Solution**: Check if database detection logic is working correctly

### Database Inspection
```sql
-- Check folders
SELECT * FROM folders;

-- Check documents
SELECT * FROM documents;

-- Check folder hierarchy
SELECT f1.name as parent, f2.name as child 
FROM folders f1 
JOIN folders f2 ON f1.id = f2.parentId;
```

## Future Enhancements

1. **Proper Migration Scripts**: Replace fallback destructive migration with proper schema migration
2. **Caching**: Add in-memory caching for frequently accessed folders
3. **Search**: Implement full-text search across documents
4. **Batch Operations**: Add support for bulk operations
5. **Sync**: Add cloud sync capabilities using database as source of truth

## Rollback Plan

If issues are discovered:
1. Restore original FileExplorerViewModel from version control
2. Remove Room database dependencies
3. Restore XML-based file management
4. Use backup XML files if data recovery is needed