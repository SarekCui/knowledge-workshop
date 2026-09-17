export interface UserProfile {
  userId: string;
  username: string;
  nickname: string;
  avatarUrl: string | null;
  bio: string | null;
  version: number;
}

export interface PublicUserProfile {
  userId: string;
  nickname: string;
  avatarUrl: string | null;
}
