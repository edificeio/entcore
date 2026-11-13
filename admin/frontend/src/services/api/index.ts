import { createModerationService } from './moderationService';

export const moderationBaseUrl = '/moderation/api/v1';

export const moderationService = createModerationService(moderationBaseUrl);
