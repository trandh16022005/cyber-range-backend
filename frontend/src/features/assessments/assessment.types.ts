export type AssessmentStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED';

export type AssessmentReviewStatus =
  | 'PENDING'
  | 'REVIEWED';

export type AssessmentResultStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'FAILED'
  | 'NOT_APPLICABLE';

export interface Assessment {
  id: number;
  exerciseId: number;
  teamId: number;
  teamName: string;
  status: AssessmentStatus;
  totalScore: number;
  startedAt: string | null;
  completedAt: string | null;
  reviewStatus: AssessmentReviewStatus;
  reviewedById: number | null;
  reviewedAt: string | null;
  reviewComment: string | null;
  createdAt: string;
}

export interface AssessmentScore {
  id: number;
  criterionId: number;
  criterionName: string;
  criterionType: string;
  awardedScore: number;
  maxScore: number;
  reason: string | null;
}

export interface AssessmentResult {
  id: number;
  assessmentId: number;
  defensiveTaskId: number;
  defensiveTaskName: string;
  sequenceOrder: number;
  status: AssessmentResultStatus;
  startedAt: string | null;
  detectedAt: string | null;
  respondedAt: string | null;
  completedAt: string | null;
  detectionTimeSeconds: number | null;
  responseTimeSeconds: number | null;
  reason: string | null;
  awardedScore: number;
  maxScore: number;
  scores: AssessmentScore[];
}

export interface CompleteAssessmentResultRequest {
  reason?: string;
}