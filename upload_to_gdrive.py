import os
import sys
import base64
import json
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

def upload_file(file_path, folder_id, credentials_json):
    try:
        creds_dict = json.loads(base64.b64decode(credentials_json))
        creds = service_account.Credentials.from_service_account_info(creds_dict)
        service = build('drive', 'v3', credentials=creds)

        file_name = 'IdeasTracker.apk'

        # 1. Search for existing file
        query = f"name = '{file_name}' and '{folder_id}' in parents and trashed = false"
        results = service.files().list(q=query, spaces='drive', fields='files(id)', supportsAllDrives=True, includeItemsFromAllDrives=True).execute()
        items = results.get('files', [])

        media = MediaFileUpload(file_path, mimetype='application/vnd.android.package-archive', resumable=True)

        if items:
            # 2. Update existing file
            file_id = items[0]['id']
            print(f"Updating existing file: {file_id}")
            file = service.files().update(
                fileId=file_id,
                media_body=media,
                supportsAllDrives=True
            ).execute()
        else:
            # 3. Create new file
            print("Creating new file")
            file_metadata = {
                'name': file_name,
                'parents': [folder_id]
            }
            file = service.files().create(
                body=file_metadata,
                media_body=media,
                fields='id',
                supportsAllDrives=True
            ).execute()

        print(f"File ID: {file.get('id')}")
        return True
    except Exception as e:
        print(f"Detailed Error: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python upload_to_gdrive.py <file_path> <folder_id> <credentials_base64>")
        sys.exit(1)

    success = upload_file(sys.argv[1], sys.argv[2], sys.argv[3])
    if not success:
        sys.exit(1)
