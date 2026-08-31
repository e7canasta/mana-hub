"""Re-export — usar mana_sdk.client."""

from mana_sdk.client import ManaClient as HubClient, ManaError as HubError

__all__ = ["HubClient", "HubError"]
