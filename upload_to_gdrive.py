import os
import sys
import base64
import json
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

def upload_and_transfer(file_path, folder_id, credentials_json, owner_email):
    try:
        creds_dict = json.loads(base64.b64decode(credentials_json))
        creds = service_account.Credentials.from_service_account_info(creds_dict)
        service = build('drive', 'v3', credentials=creds)

        file_name = 'IdeasTracker.apk'

        # 1. Upload the file
        print("Uploading file...")
        file_metadata = {
            'name': file_name,
            'parents': [folder_id]
        }
        media = MediaFileUpload(file_path, mimetype='application/vnd.android.package-archive', resumable=True)

        file = service.files().create(
            body=file_metadata,
            media_body=media,
            fields='id',
            supportsAllDrives=True
        ).execute()

        file_id = file.get('id')
        print(f"File uploaded. ID: {file_id}")

        # 2. Transfer ownership
        if owner_email:
            print(f"Transferring ownership to {owner_email}...")
            # We first need to make sure the user has permission to the file, then transfer.
            # But since they own the parent folder and the SA is an editor, it might already be there.
            # Actually, to transfer ownership, we create a new permission with role 'owner'.
            permission = {
                'role': 'owner',
                'type': 'user',
                'emailAddress': owner_email
            }
            # transferOwnership=True is required for changing role to owner
            service.permissions().create(
                fileId=file_id,
                body=permission,
                transferOwnership=True,
                supportsAllDrives=True
            ).execute()
            print("Ownership transferred successfully.")

        return True
    except Exception as e:
        print(f"Detailed Error: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: python upload_to_gdrive.py <file_path> <folder_id> <credentials_base64> <owner_email>")
        sys.exit(1)

    success = upload_and_transfer(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
    if not success:
        sys.exit(1)
