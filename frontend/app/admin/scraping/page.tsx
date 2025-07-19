"use client";

import { useState } from "react";

export default function ScrapingAdminPage() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleStartScraping = async () => {
    setLoading(true);
    setMessage("");
    setError("");

    try {
      const response = await fetch(
        "http://localhost:8080/api/scraping/scheduling/jobs/daily/trigger",
        {
          method: "POST",
        }
      );

      if (response.ok) {
        const responseText = await response.text();
        setMessage(responseText);
      } else {
        const errorText = await response.text();
        setError(`Failed to start scraping job: ${errorText}`);
      }
    } catch (err) {
      setError(
        "An unexpected error occurred. Please check the console for more details."
      );
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-lg shadow-md">
        <h1 className="text-2xl font-bold text-center text-gray-900">
          Scraping Job Administration
        </h1>
        <p className="text-center text-gray-600">
          Manually trigger the daily scraping job. This will start the process
          of collecting new ideas from all configured sources.
        </p>

        <button
          onClick={handleStartScraping}
          disabled={loading}
          className="w-full px-4 py-2 text-white bg-indigo-600 rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-indigo-300 disabled:cursor-not-allowed">
          {loading ? "Starting..." : "Start Scraping Job"}
        </button>

        {message && (
          <div className="p-4 mt-4 text-green-700 bg-green-100 border border-green-400 rounded-md">
            {message}
          </div>
        )}

        {error && (
          <div className="p-4 mt-4 text-red-700 bg-red-100 border border-red-400 rounded-md">
            {error}
          </div>
        )}
      </div>
    </div>
  );
}
