import os
import sys
import json
import requests
from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from google.auth.transport.requests import Request

def upload_with_oauth(file_path, folder_id, client_id, client_secret, refresh_token):
    try:
        # Create credentials object
        creds = Credentials(
            None,
            refresh_token=refresh_token,
            token_uri="https://oauth2.googleapis.com/token",
            client_id=client_id,
            client_secret=client_secret,
            scopes=["https://www.googleapis.com/auth/drive.file"]
        )

        # Refresh token if needed
        if not creds.valid:
            creds.refresh(Request())

        service = build('drive', 'v3', credentials=creds)

        file_name = 'IdeasTracker.apk'

        # 1. Search for existing file
        query = f"name = '{file_name}' and '{folder_id}' in parents and trashed = false"
        results = service.files().list(q=query, spaces='drive', fields='files(id)').execute()
        items = results.get('files', [])

        media = MediaFileUpload(file_path, mimetype='application/vnd.android.package-archive', resumable=True)

        if items:
            # 2. Update existing file
            file_id = items[0]['id']
            print(f"Updating existing file: {file_id}")
            file = service.files().update(
                fileId=file_id,
                media_body=media
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
                fields='id'
            ).execute()

        print(f"Upload Successful. File ID: {file.get('id')}")
        return True
    except Exception as e:
        print(f"Detailed Error: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 6:
        print("Usage: python upload_with_oauth.py <file_path> <folder_id> <client_id> <client_secret> <refresh_token>")
        sys.exit(1)

    success = upload_with_oauth(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
    if not success:
        sys.exit(1)
