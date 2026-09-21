import { useEffect, useState } from "react";
import { getCurrentUser } from "../api/UserApi";
import type { User } from "../types/User";

function Home() {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const loadUser = async () => {
      try {
        const data = await getCurrentUser();
        setUser(data);
      } catch (error) {
        console.error("Failed to load user:", error);
      }
    };

    loadUser();
  }, []);

  return (
    <div>
      <h1>Home</h1>

      {user && (
        <div>
          <strong>{user.username}</strong>
          <p>{user.bio}</p>
        </div>
      )}
    </div>
  );
}

export default Home;